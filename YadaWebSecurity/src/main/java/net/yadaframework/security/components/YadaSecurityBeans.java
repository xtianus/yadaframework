package net.yadaframework.security.components;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import net.yadaframework.core.YadaConfiguration;

/**
 *
 */
@Configuration
public class YadaSecurityBeans {
	// This class contains bean factories that were previously in YadaSecurityConfig but prevented to have more than one subclass

	@Bean(name="passwordEncoder")
	PasswordEncoder passwordEncoder(YadaConfiguration yadaConfiguration) {
		if (yadaConfiguration.encodePassword()) {
			return new BCryptPasswordEncoder();
		}
		return null;
	}

}
