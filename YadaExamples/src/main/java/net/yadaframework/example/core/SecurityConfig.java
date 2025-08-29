package net.yadaframework.example.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import net.yadaframework.security.YadaSecurityConfig;

/**
 * Security configuration.
 * You only need to set the roles for the application paths.
 * Other configuration is set on YadaSecurityConfig
 * @see YadaSecurityConfig
 */
@Configuration
@EnableWebSecurity
//@DependsOn("webConfig") // Fix circular reference on 'mvcContentNegotiationManager' creation
public class SecurityConfig extends YadaSecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		//	The mapping matches URLs using the following rules:
		//		? matches one character
		//		* matches zero or more characters
		//		** matches zero or more 'directories' in a path
		//	Patterns which end with /** (and have no other wildcards) are optimized by using a substring match
        http.authorizeHttpRequests(authorize -> authorize
    		.requestMatchers("/dashboard/userwrite/**").hasAnyRole("SUPERVISOR", "ADMIN")
    		.requestMatchers("/dashboard/user/deimpersonate").authenticated()
    		.requestMatchers("/dashboard/user/**").hasAnyRole("SUPERVISOR", "ADMIN")
			.requestMatchers("/dashboard/**").hasAnyRole("SUPERVISOR", "ADMIN")
			.requestMatchers("/my/**").hasAnyRole("USER")
		);

        super.configure(http);

        // anyRequest().permitAll() mapping must be the last one
        http.authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
