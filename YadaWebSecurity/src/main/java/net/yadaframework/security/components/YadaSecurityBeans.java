package net.yadaframework.security.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import net.yadaframework.core.YadaConfiguration;

/**
 *
 */
@Configuration
public class YadaSecurityBeans {
	// This class contains bean factories that were previously in YadaSecurityConfig but prevented to have more than one subclass
	@Autowired private YadaConfiguration yadaConfiguration;

	// This is only used by spring security filter configured in SecurityWebApplicationInitializer
//	TODO: @Bean(name="filterMultipartResolver")
//	CommonsMultipartResolver filterMultipartResolver() {
//		CommonsMultipartResolver filterMultipartResolver = new YadaCommonsMultipartResolver();
//		filterMultipartResolver.setMaxUploadSize(yadaConfiguration.getMaxFileUploadSizeBytes());
//		// filterMultipartResolver.setResolveLazily(true);
//		return filterMultipartResolver;
//	}

	@Bean(name="passwordEncoder")
	@Autowired PasswordEncoder passwordEncoder(YadaConfiguration yadaConfiguration) {
		if (yadaConfiguration.encodePassword()) {
			return new BCryptPasswordEncoder();
		}
		return null;
	}


}
