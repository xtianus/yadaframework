package net.yadaframework.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import net.yadaframework.core.YadaConfiguration;

// Queste annotation non servono
// @EnableWebMvcSecurity
@Configuration
@EnableJpaRepositories(basePackages = "net.yadaframework.security.persistence.repository")
@ComponentScan(basePackages = { "net.yadaframework.security" })
public abstract class YadaSecurityConfig extends WebSecurityConfigurerAdapter {
//	private Logger log = LoggerFactory.getLogger(YadaSecurityConfig.class);

	@Autowired private YadaUserDetailsService userDetailsService;
	@Autowired private YadaConfiguration yadaConfiguration;

	@Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        // Uso un PostProcessor per chiamare setHideUserNotFoundExceptions
        auth.userDetailsService(userDetailsService).addObjectPostProcessor(new ObjectPostProcessor<DaoAuthenticationProvider>() {
			@Override
			public DaoAuthenticationProvider postProcess(DaoAuthenticationProvider processor) {
				processor.setHideUserNotFoundExceptions(false); // Permette alla UsernameNotFoundException di arrivare al FailureHandler
				if (yadaConfiguration.encodePassword()) {
					processor.setPasswordEncoder(passwordEncoder(yadaConfiguration));
				}
				return processor;
			}
		});
    }

	abstract protected void configure(HttpSecurity http) throws Exception;

	// TODO not sure why this has to be in WebSecurity [xtian]
	@Bean(name="filterMultipartResolver")
	CommonsMultipartResolver filterMultipartResolver() {
		CommonsMultipartResolver filterMultipartResolver = new YadaCommonsMultipartResolver();
		filterMultipartResolver.setMaxUploadSize(yadaConfiguration.getMaxFileUploadSizeBytes());
		// filterMultipartResolver.setResolveLazily(true);
		return filterMultipartResolver;
	}
	
	@Bean(name="passwordEncoder")
	@Autowired PasswordEncoder passwordEncoder(YadaConfiguration yadaConfiguration) {
		if (yadaConfiguration.encodePassword()) {
			return new BCryptPasswordEncoder();
		}
		return null;
	}
	
}
