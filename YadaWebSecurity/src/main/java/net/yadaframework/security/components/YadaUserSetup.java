package net.yadaframework.security.components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.thymeleaf.util.StringUtils;

import net.yadaframework.components.YadaSetup;
import net.yadaframework.exceptions.YadaConfigurationException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.persistence.repository.YadaUserProfileDao;

/**
 * Convenience method to create configured application users.
 * You should just create a @Component that extends this class and add a setup section to the configuration
 * with any user attribute you have implemented in your YadaUserProfile subclass.
 * For example:
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

 *
 * @param <T> the application-specific extension of YadaUserProfile
 */
abstract public class YadaUserSetup<T extends YadaUserProfile> extends YadaSetup {
	private transient Logger log = LoggerFactory.getLogger(YadaUserSetup.class);

	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaUserProfileDao<T> yadaUserProfileDao;
	@Autowired private PasswordEncoder encoder;
	
	@Override
	protected void setupApplication() {
		// Default empty method
	}

	@Override
	protected void setupUsers(List<Map<String, Object>> userList) {
		// Retrieve the generics YadaUserProfile subclass: https://stackoverflow.com/a/75345/587641 
		Class userProfileClass = (Class)((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		//
		for (Map<String, Object> userDefinition : userList) {
			// The map is a key-value pair of user attributes except "roles" which is a list of role ids
			String email = (String) userDefinition.get("email");
			String password = (String) userDefinition.get("password");
			if (email==null || password==null) {
				throw new YadaConfigurationException("setup users must have <email> and <password> elements");
			}
			YadaUserCredentials existingUserCredentials = yadaUserCredentialsDao.findFirstByUsername(email);
			if (existingUserCredentials == null) {
				log.info("Setup: creating user {}", email);
				T userProfile;
				try {
					userProfile = (T) userProfileClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e1) {
					log.error("Failed to setup user of type {}", userProfileClass, e1);
					throw new YadaInvalidUsageException("Invalid user type {}", userProfileClass);
				}
				YadaUserCredentials userCredentials = new YadaUserCredentials();
				userCredentials.setUsername(email);
				userCredentials.changePassword(password, encoder);
				userCredentials.setEnabled(true);
				userProfile.setUserCredentials(userCredentials);
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
				userProfile = yadaUserProfileDao.save(userProfile);
				yadaUserCredentialsDao.save(userCredentials);
			}
		}
	}

}
