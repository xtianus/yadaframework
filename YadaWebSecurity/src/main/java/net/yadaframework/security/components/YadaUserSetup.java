package net.yadaframework.security.components;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.yadaframework.components.YadaSetup;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;

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
//Not a @Component
abstract public class YadaUserSetup<T extends YadaUserProfile> extends YadaSetup {
	private transient Logger log = LoggerFactory.getLogger(YadaUserSetup.class);

	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	
	@Override
	protected void setupApplication() {
		// Default empty method
		log.debug("setupApplication() does nothing");
	}

	@Override
	protected void setupUsers(List<Map<String, Object>> userList) {
		// Retrieve the generics YadaUserProfile subclass: https://stackoverflow.com/a/75345/587641 
		Class<T> userProfileClass = (Class<T>)((ParameterizedType)this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		//
		for (Map<String, Object> userDefinition : userList) {
			yadaUserCredentialsDao.create(userDefinition, userProfileClass);
		}
	}

}
