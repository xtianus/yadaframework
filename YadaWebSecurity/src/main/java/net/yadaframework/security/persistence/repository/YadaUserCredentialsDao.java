package net.yadaframework.security.persistence.repository;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.util.StringUtils;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.exceptions.YadaConfigurationException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.web.YadaPageRequest;

/**
 * YadaUserCredentials database operations.
 * Converted from YadaUserCredentialsRepository before deleting it.
 */
@Repository
@Transactional(readOnly = true)
public class YadaUserCredentialsDao {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

    @PersistenceContext
	private EntityManager em;

	@Autowired private PasswordEncoder encoder;
	@Autowired private YadaUtil yadaUtil;

	/**
	 * Change the password
	 * @param yadaUserCredentials
	 * @param password
	 * @return the updated object
	 * @see YadaUserCredentials#changePassword(String, PasswordEncoder)
	 */
	@Transactional(readOnly = false)
	public YadaUserCredentials changePassword(YadaUserCredentials yadaUserCredentials, String password) {
		yadaUserCredentials = em.merge(yadaUserCredentials);
		yadaUserCredentials.changePassword(password, encoder);
		return yadaUserCredentials;
	}

	/**
	 * Creates a user when it doesn't exists, using the configured attributes.
	 * The user class must extend YadaUserProfile and implement the getter/setter of all attributes specified in the configuration and not already implemented
	 * by its superclass.
	 * Example configuration:
	 * <pre>
	 * 	&lt;setup>
			&lt;users>
				&lt;user>
					&lt;name>admin&lt;/name>
					&lt;email>admin@somemail.com&lt;/email>
					&lt;password>25345352543154&lt;/password>
					&lt;language>en&lt;/language>
					&lt;country>US&lt;/country>
					&lt;timezone>PST&lt;/timezone>
					&lt;role>USER&lt;/role>
					&lt;role>ADMIN&lt;/role>
				&lt;/user>
			&lt;/users>
		&lt;/setup>
		</pre>
	 * @param userDefinition
	 * @return the created user if it didn't exist already, null otherwise
	 */
    @Transactional(readOnly = false)
    public <T extends YadaUserProfile> T create(Map<String, Object> userDefinition, Class<T> userProfileClass) {
		// The map is a key-value pair of user attributes except "roles" which is a list of role ids
		String email = (String) userDefinition.get("email");
		if (email==null) {
			throw new YadaConfigurationException("Configured users must have an <email>");
		}
		String password = (String) userDefinition.get("password");
		if (password==null) {
			// When there is no password set, use a strong random password
			password = yadaUtil.getRandomText(20); // Random password with 20 characters minimum
		}
		YadaUserCredentials existingUserCredentials = this.findFirstByUsername(email);
		if (existingUserCredentials == null) {
			log.info("Setup: creating user {}", email);
			try {
				T userProfile = userProfileClass.newInstance();
				YadaUserCredentials userCredentials = new YadaUserCredentials();
				userCredentials.setUsername(email);
				userCredentials.changePassword(password, encoder);
				userCredentials.setEnabled(true);
				userProfile.setUserCredentials(userCredentials);
//				String language = (String) userDefinition.get("language");
//				String country = (String) userDefinition.get("country");
//				if (language!=null && country!=null) {
//					userProfile.setLocale(language.toLowerCase() + "_" + country.toUpperCase());
//				}
				em.persist(userProfile); // Cascade to userCredentials
				for (String key : userDefinition.keySet()) {
					Object valueObject = userDefinition.get(key);
					if (key.equals("roles")) {
						for (Integer role : (Set<Integer>)valueObject) {
							userCredentials.addRole(role);
						}
					} else if (!key.equals("email") && !key.equals("password")) {
						String setterName = "set" + StringUtils.capitalize(key); // e.g. setTimeZone
						Method setter = null;
						try {
							try {
								setter = userProfileClass.getMethod(setterName, String.class);
							} catch (NoSuchMethodException e) {
								// Try a different version?
								if (!key.toLowerCase().equals(key)) {
									setterName = "set" + StringUtils.capitalize(key.toLowerCase()); // e.g. setTimezone
									setter = userProfileClass.getMethod(setterName, String.class);
								} else {
									throw e;
								}
							}
							setter.invoke(userProfile, (String)valueObject);
						} catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							log.error("Can't set attribute '{}' on {} (skipped)", key, userProfileClass, e);
						}
					}

				}
				return userProfile;
			} catch (InstantiationException | IllegalAccessException e1) {
				log.error("Failed to setup user of type {}", userProfileClass, e1);
				throw new YadaInvalidUsageException("Invalid user type {}", userProfileClass);
			}
		}
		return null;
    }

	/**
	 * Create a new YadaUserCredentials object that holds login information for the user
	 * @param username
	 * @param password
	 * @param roles
	 * @param timezone
	 * @return
	 */
    @Transactional(readOnly = false)
    public YadaUserCredentials create(String username, String password, Set<Integer> roles) {
		YadaUserCredentials userCredentials = new YadaUserCredentials();
		em.persist(userCredentials);
		//
		userCredentials.setUsername(username);
		userCredentials.changePassword(password, encoder);
		userCredentials.setEnabled(true);
		for (Integer role : roles) {
			userCredentials.addRole(role);
		}
		return userCredentials;
    }

	/**
	 * Find the credentials for the given user profile id
	 * @param userProfileId
	 * @return the element, or null when no element exists
	 */
	public YadaUserCredentials findByUserProfileId(Long userProfileId) {
		String sql = "select yuc from YadaUserProfile up join up.userCredentials yuc where up.id = :userProfileId";
		List<YadaUserCredentials> resultList = em.createQuery(sql, YadaUserCredentials.class)
			.setMaxResults(1)
			.setParameter("userProfileId", userProfileId)
			.getResultList();
		return normaliseSingleResult(resultList);
	}

	/**
	 * Find the first user with the given username
	 * @param username
	 * @return the element, or null when no element exists
	 */
	public YadaUserCredentials findFirstByUsername(String username) {
		String sql = "select yuc from YadaUserCredentials yuc where yuc.username = :username";
		List<YadaUserCredentials> resultList = em.createQuery(sql, YadaUserCredentials.class)
			.setMaxResults(1)
			.setParameter("username", username)
			.getResultList();
		return normaliseSingleResult(resultList);
	}

	/**
	 * Find a user with the given username
	 * @param username
	 * @param pageable the page/size parameters or YadaPageRequest.FIND_ONE for the first result only
	 * @return the list of users found
	 * @deprecated because there can be no more than one result
	 * @see #findFirstByUsername(String)
	 */
	@Deprecated
	public List<YadaUserCredentials> findByUsername(String username, YadaPageRequest pageable) {
		String sql = "select yuc from YadaUserCredentials yuc where yuc.username = :username";
		if (pageable==null || !pageable.isValid()) {
			log.debug("Invalid page request");
			return new ArrayList<YadaUserCredentials>();
		}
		sql += " " + YadaSql.getOrderByNative(pageable);
		List<YadaUserCredentials> resultList = em.createQuery(sql, YadaUserCredentials.class)
			.setParameter("username", username)
			.setFirstResult(pageable.getOffset())
			.setMaxResults(pageable.getSize())
			.getResultList();
		return resultList;
	}

	/**
	 * Find the list of users with the given username.
	 * @param username
	 * @return
	 * @deprecated because there can be no more than one result
	 * @see #findFirstByUsername(String)
	 */
	@Deprecated
	public List<YadaUserCredentials> findByUsername(String username) {
		String sql = "select yuc from YadaUserCredentials yuc where yuc.username = :username";
		List<YadaUserCredentials> resultList = em.createQuery(sql, YadaUserCredentials.class)
				.setParameter("username", username)
				.getResultList();
			return resultList;
	}

	/**
	 * Updates the login timestamp of the user
	 * @param username
	 */
	@Transactional(readOnly = false)
	public void updateLoginTimestamp(String username) {
		String sql = "update YadaUserCredentials e set e.lastSuccessfulLogin = NOW() where e.username = :username";
		em.createQuery(sql).setParameter("username", username).executeUpdate();
	}

	/**
	 * Updates the login failed attempts counter for the user
	 * @param username
	 */
	@Transactional(readOnly = false)
	public void incrementFailedAttempts(String username) {
		String sql = "update YadaUserCredentials e set e.failedAttempts = e.failedAttempts + 1, e.lastFailedAttempt = NOW() where e.username = :username";
		em.createQuery(sql).setParameter("username", username).executeUpdate();
	}

	/**
	 * Resets the login failed attempts counter for the user
	 * @param username
	 */
	@Transactional(readOnly = false)
	public void resetFailedAttempts(String username) {
		String sql = "update YadaUserCredentials e set e.failedAttempts = 0, e.lastFailedAttempt = null where e.username = :username";
		em.createQuery(sql).setParameter("username", username).executeUpdate();
	}

	/**
	 * Saves the parameter. Implemented for compatibility with the old YadaUserCredentialsRepository.save() method.
	 * @param userCredentials
	 * @deprecated use a higher-level method instead
	 * @see #create(String, String, Set)
	 */
	@Deprecated
	@Transactional(readOnly = false)
	public void save(YadaUserCredentials userCredentials) {
		userCredentials = em.merge(userCredentials);
	}

    /**
     * For backwards compatibility, returns null when no result is found
     * @param resultList
     * @return
     */
    private YadaUserCredentials normaliseSingleResult(List<YadaUserCredentials> resultList) {
		// Need to keep the contract of the Spring Data Repository, so we return null when no value found.
		if (resultList.isEmpty()) {
			return null;
		} else {
			return resultList.get(0);
		}
    }

}
