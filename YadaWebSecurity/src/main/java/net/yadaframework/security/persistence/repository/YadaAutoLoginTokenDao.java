package net.yadaframework.security.persistence.repository;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.components.YadaSecurityTokenCodec;
import net.yadaframework.security.components.YadaSecurityTokenCodec.ParsedSecurityToken;
import net.yadaframework.security.persistence.entity.YadaAutoLoginToken;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;

/**
 * Issues, validates and consumes autologin tokens. A token is valid while it exists, matches the digest, is not
 * expired (a missing expiration is invalid) and its user is enabled. {@link #consume(ParsedSecurityToken)} locks and
 * deletes the row, so a token logs in at most once also across application instances.
 */
@Repository
@Transactional(readOnly = true) 
public class YadaAutoLoginTokenDao {
	/** The purpose of the autologin token digests */
	public static final String PURPOSE = "AUTOLOGIN";
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@PersistenceContext
	EntityManager em;
	@Autowired private YadaSecurityTokenCodec yadaSecurityTokenCodec;

	/**
	 * Stores a new token. The secret is available with {@link YadaAutoLoginToken#getRawSecret()} on the returned
	 * instance only, to build the link.
	 * @param yadaUserCredentials the user that the link logs in
	 * @param expiration the time from which the token can't be used, required
	 * @return the new token
	 */
	@Transactional(readOnly = false)
	public YadaAutoLoginToken issue(YadaUserCredentials yadaUserCredentials, Date expiration) {
		if (yadaUserCredentials==null) {
			throw new YadaInvalidUsageException("The user of an autologin token is required");
		}
		if (expiration==null) {
			throw new YadaInvalidUsageException("Autologin tokens must have an expiration");
		}
		YadaAutoLoginToken yadaAutoLoginToken = new YadaAutoLoginToken();
		yadaAutoLoginToken.setExpiration(expiration);
		yadaAutoLoginToken.setYadaUserCredentials(yadaUserCredentials);
		String secret = yadaSecurityTokenCodec.generateSecret();
		yadaAutoLoginToken.applyIssuedToken(secret, yadaSecurityTokenCodec.digest(PURPOSE, secret));
		em.persist(yadaAutoLoginToken);
		return yadaAutoLoginToken;
	}

	private boolean isValid(YadaAutoLoginToken yadaAutoLoginToken, ParsedSecurityToken token) {
		if (yadaAutoLoginToken==null) {
			log.debug("No autologin token with id {}", token.id());
			return false;
		}
		if (!yadaSecurityTokenCodec.matches(PURPOSE, token.secret(), yadaAutoLoginToken.getTokenHash())) {
			log.debug("Wrong secret for autologin token {}", token.id());
			return false;
		}
		if (YadaSecurityTokenCodec.isExpired(yadaAutoLoginToken.getExpiration(), new Date())) {
			log.debug("Autologin token {} expired", token.id());
			return false;
		}
		YadaUserCredentials yadaUserCredentials = yadaAutoLoginToken.getYadaUserCredentials();
		if (yadaUserCredentials==null || !yadaUserCredentials.isEnabled()) {
			log.debug("The user of autologin token {} is missing or disabled", token.id());
			return false;
		}
		return true;
	}

	/**
	 * Finds the token of a link without using it, e.g. to show the confirmation page
	 * @param token the parsed link, can be null
	 * @return the token, with its user, when it is valid; null otherwise
	 */
	public YadaAutoLoginToken findValid(ParsedSecurityToken token) {
		if (token==null) {
			return null;
		}
		YadaAutoLoginToken yadaAutoLoginToken = em.find(YadaAutoLoginToken.class, token.id());
		return isValid(yadaAutoLoginToken, token) ? yadaAutoLoginToken : null;
	}

	/**
	 * Uses a token once: locks its row (SELECT ... FOR UPDATE), validates it and deletes it in one transaction.
	 * Concurrent calls wait for the lock and then find no token. The caller logs the user in after this method returns;
	 * if that fails, a new link is needed.
	 * @param token the parsed link, can be null
	 * @return the enabled user of a valid token, or null
	 */
	@Transactional(readOnly = false)
	public YadaUserCredentials consume(ParsedSecurityToken token) {
		if (token==null) {
			return null;
		}
		YadaAutoLoginToken yadaAutoLoginToken = em.find(YadaAutoLoginToken.class, token.id(), LockModeType.PESSIMISTIC_WRITE);
		if (!isValid(yadaAutoLoginToken, token)) {
			return null;
		}
		YadaUserCredentials yadaUserCredentials = yadaAutoLoginToken.getYadaUserCredentials();
		yadaUserCredentials.getRoles().size(); // Loaded before the transaction ends
		em.remove(yadaAutoLoginToken);
		return yadaUserCredentials;
	}
	
	/**
	 * Returns the valid tokens of a user
	 * @param YadaUserCredentials
	 * @return
	 */
	public List<YadaAutoLoginToken> findByYadaUserCredentials(YadaUserCredentials yadaUserCredentials) {
		String sql = "from YadaAutoLoginToken e where e.yadaUserCredentials=:yadaUserCredentials and e.expiration > :now";
		return em.createQuery(sql, YadaAutoLoginToken.class)
			.setParameter("yadaUserCredentials", yadaUserCredentials)
			.setParameter("now", new Date())
			.getResultList();
	}

	/**
	 * Delete expired elements, including those without expiration. This is maintenance: expired tokens are
	 * rejected anyway.
	 */
    @Transactional(readOnly = false)
	public void deleteExpired() {
		String sql = "delete from YadaAutoLoginToken e where e.expiration is null or e.expiration <= :now";
		em.createQuery(sql).setParameter("now", new Date()).executeUpdate();
	}
    
	/**
	 * Updates an existing token, e.g. its expiration. New tokens must be created with {@link #issue(YadaUserCredentials, Date)}.
	 * @throws YadaInvalidUsageException for a new token
	 */
	@Transactional(readOnly = false)
	public YadaAutoLoginToken save(YadaAutoLoginToken entity) {
		if (entity==null) {
			return null;
		}
		if (entity.getId()==null) {
			throw new YadaInvalidUsageException("New autologin tokens must be created with issue()");
		}
		return em.merge(entity);
	}

}
