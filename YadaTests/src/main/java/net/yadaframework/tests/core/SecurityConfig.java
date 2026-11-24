package net.yadaframework.tests.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import net.yadaframework.security.YadaSecurityConfig;

/**
 * Security configuration of the YadaTests application: only the scenario paths need a role.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig extends YadaSecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(authorize -> authorize
			.requestMatchers("/test/admin/**").hasRole("ADMIN")
			.requestMatchers("/test/user/**").hasRole("USER")
		);
		super.configure(http);
		// anyRequest().permitAll() must be the last mapping
		http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
		return http.build();
	}
}
