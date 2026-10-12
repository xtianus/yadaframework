package net.yadaframework.security.uploadtest.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import net.yadaframework.security.YadaSecurityConfig;

/**
 * Security configuration for the upload limit integration tests.
 */
@Configuration
@EnableWebSecurity
public class UploadLimitTestSecurityConfig extends YadaSecurityConfig {

	/**
	 * Builds the security filter chain while keeping CSRF enabled.
	 * @param http security builder
	 * @return configured security filter chain
	 * @throws Exception if configuration fails
	 */
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		super.configure(http);
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		return http.build();
	}
}
