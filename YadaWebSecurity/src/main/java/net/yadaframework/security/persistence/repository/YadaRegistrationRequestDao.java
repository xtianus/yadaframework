package net.yadaframework.security.persistence.repository;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.components.YadaSecurityTokenCodec;
import net.yadaframework.security.components.YadaSecurityTokenCodec.ParsedSecurityToken;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;

/**
 * Issues, validates and consumes registration, password recovery and email change requests.
 * <p>
 * A request is created only by {@link #issue(YadaRegistrationRequest)}, which generates the token secret, stores its
 * digest and sets the expiration for the request purpose. A link is valid while its request exists, has the expected
 * {@link YadaRegistrationType}, matches the digest and is not expired. {@link #consume(ParsedSecurityToken, YadaRegistrationType, Class, Function)}
 * locks the row, so a link is used at most once also across application instances.
 */
@Repository
@Transactional(readOnly = true) 
public class YadaRegistrationRequestDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@PersistenceContext EntityManager em;
	@Autowired private YadaSecurityTokenCodec yadaSecurityTokenCodec;
	@Autowired private YadaConfiguration config;

	/**
	 * Stores a new request with a fresh token. The secret is available with {@link YadaRegistrationRequest#getRawSecret()}
	 * on the returned instance only, to build the link of the email.
	 * The request must be new: an id would let a caller overwrite an existing request.
	 * @param request a new request with email, password (for registrations) and registration type set
	 * @return the same instance, persisted
	 * @throws YadaInvalidUsageException when the request has an id or no registration type
	 */
	@Transactional(readOnly = false)
	public <R extends YadaRegistrationRequest> R issue(R request) {
		if (request==null || request.getId()!=null) {
			throw new YadaInvalidUsageException("Only a new registration request without id can be issued");
		}
		YadaRegistrationType purpose = request.getRegistrationType();
		if (purpose==null) {
			throw new YadaInvalidUsageException("The registration type of a new registration request is required");
		}
		String secret = yadaSecurityTokenCodec.generateSecret();
		Date now = new Date();
		Date expiration = new Date(now.getTime() + getLifetimeMillis(purpose));
		request.applyIssuedToken(secret, yadaSecurityTokenCodec.digest(purpose.name(), secret), now, expiration);
		em.persist(request);
		return request;
	}

	/**
	 * @param purpose the request purpose
	 * @return the configured lifetime of a new request with that purpose, in milliseconds
	 */
	public long getLifetimeMillis(YadaRegistrationType purpose) {
		switch (purpose) {
		case PASSWORD_RECOVERY:
			return config.getPasswordResetExpirationMinutes() * 60_000L;
		case EMAIL_CHANGE:
			return config.getEmailChangeExpirationMinutes() * 60_000L;
		default:
			return config.getRegistrationExpirationHours() * 3600_000L;
		}
	}

	private boolean isValid(YadaRegistrationRequest request, ParsedSecurityToken token, YadaRegistrationType purpose) {
		if (request==null) {
			log.debug("No registration request with id {}", token.id());
			return false;
		}
		if (request.getRegistrationType()!=purpose) {
			log.debug("Registration request {} has type {} instead of {}", token.id(), request.getRegistrationType(), purpose);
			return false;
		}
		if (!yadaSecurityTokenCodec.matches(purpose.name(), token.secret(), request.getTokenHash())) {
			log.debug("Wrong secret for registration request {}", token.id());
			return false;
		}
		if (YadaSecurityTokenCodec.isExpired(request.getExpiration(), new Date())) {
			log.debug("Registration request {} expired", token.id());
			return false;
		}
		return true;
	}

	/**
	 * Finds the request of a link without using it, e.g. to show the password recovery form
	 * @param token the parsed link, can be null
	 * @param purpose the expected type of the request
	 * @param type the request class
	 * @return the request when the link is valid for that purpose, null otherwise
	 */
	public <R extends YadaRegistrationRequest> R findValid(ParsedSecurityToken token, YadaRegistrationType purpose, Class<R> type) {
		if (token==null || purpose==null) {
			return null;
		}
		R request = em.find(type, token.id());
		return isValid(request, token, purpose) ? request : null;
	}

	/**
	 * Uses a link once, in one transaction: locks the request row (SELECT ... FOR UPDATE), validates it, deletes it and
	 * runs the action, which can call other DAOs in the same transaction (e.g. to change a password). When the action
	 * throws, everything is rolled back and the link is still usable. Concurrent calls for the same link wait for the
	 * lock and then find no request, so only one of them runs the action. Anything that must happen after the commit,
	 * like logging the user in, must be done by the caller after this method returns.
	 * @param token the parsed link, can be null
	 * @param purpose the expected type of the request
	 * @param type the request class
	 * @param action what the link is for; it receives the request and must not return null
	 * @return the result of the action, or empty when the link is not valid for that purpose
	 */
	@Transactional(readOnly = false)
	public <R extends YadaRegistrationRequest, T> Optional<T> consume(ParsedSecurityToken token, YadaRegistrationType purpose, Class<R> type, Function<R, T> action) {
		if (token==null || purpose==null) {
			return Optional.empty();
		}
		R request = em.find(type, token.id(), LockModeType.PESSIMISTIC_WRITE);
		if (!isValid(request, token, purpose)) {
			return Optional.empty();
		}
		em.remove(request);
		em.flush();
		T result = action.apply(request);
		return Optional.of(Objects.requireNonNull(result, "The action of a registration request must not return null"));
	}

	public <R extends YadaRegistrationRequest> List<R> findByEmailAndRegistrationType(String email, YadaRegistrationType registrationType, Class<R> type) {
		String sql = "from " + type.getSimpleName() + " where email=:email and registrationType=:registrationType";
		return em.createQuery(sql, type)
			.setParameter("email", email)
			.setParameter("registrationType", registrationType)
			.getResultList();
	}

	/**
	 * @return the requests of a user with the given type, e.g. the pending email changes
	 */
	public List<YadaRegistrationRequest> findByUserAndRegistrationType(YadaUserCredentials yadaUserCredentials, YadaRegistrationType registrationType) {
		String sql = "from YadaRegistrationRequest where yadaUserCredentials=:yadaUserCredentials and registrationType=:registrationType";
		return em.createQuery(sql, YadaRegistrationRequest.class)
			.setParameter("yadaUserCredentials", yadaUserCredentials)
			.setParameter("registrationType", registrationType)
			.getResultList();
	}

	/**
	 * Trova tutti gli elementi con timestamp antecedente la data indicata
	 * @param upperLimit
	 * @return
	 */
	public List<YadaRegistrationRequest> findByTimestampBefore(Date upperLimit) {
		String sql = "from YadaRegistrationRequest where timestamp < :upperLimit";
		return em.createQuery(sql, YadaRegistrationRequest.class)
			.setParameter("upperLimit", upperLimit)
			.getResultList();
	}

	@Transactional(readOnly = false)
	public void delete(YadaRegistrationRequest registrationRequest) {
		registrationRequest = em.merge(registrationRequest);
		em.remove(registrationRequest);
	}

	/**
	 * Stores a new request with a fresh token, like {@link #issue(YadaRegistrationRequest)}.
	 * Existing requests can't be saved again, so a bound id can never overwrite a stored request.
	 * @throws YadaInvalidUsageException when the request has an id
	 * @deprecated use {@link #issue(YadaRegistrationRequest)}
	 */
	@Deprecated
	@Transactional(readOnly = false)
	public YadaRegistrationRequest save(YadaRegistrationRequest entity) {
		if (entity==null) {
			return null;
		}
		return issue(entity);
	}
}
