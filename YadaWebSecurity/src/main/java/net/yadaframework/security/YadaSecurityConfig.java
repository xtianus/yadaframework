package net.yadaframework.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	private Logger log = LoggerFactory.getLogger(YadaSecurityConfig.class);

	@Autowired private YadaUserDetailsService userDetailsService;
	@Autowired private YadaConfiguration yadaConfiguration;

	@Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
//    	// USER è un normale utente
//    	// EDITOR è un editor
//    	// ADMIN è un amministratore
//    	// MANAGER è lo sviluppatore del sito con poteri assoluti
//    	YadaRole[] allRoles = YadaRole.values();
//    	String[] allRoleStrings = new String[allRoles.length];
//    	for (int i = 0; i < allRoles.length; i++) {
//			allRoleStrings[i]=allRoles[i].name();
//		}
//        auth
//            .inMemoryAuthentication()
//                .withUser("yoda@yodadog.net").password("Gep-Petto").roles(allRoleStrings);
        
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

//    @Override
abstract protected void configure(HttpSecurity http) throws Exception;
////    	The mapping matches URLs using the following rules:
////    		? matches one character
////    		* matches zero or more characters
////    		** matches zero or more 'directories' in a path
////		Patterns which end with /** (and have no other wildcards) are optimized by using a substring match
//        http
//            .authorizeRequests()
//            	.antMatchers("/private/**").hasRole(YadaRole.USER.name())
//            	.antMatchers("/admin/**").hasRole(YadaRole.ADMIN.name())
//            	.antMatchers("/setup/**").hasRole(YadaRole.MANAGER.name())
//            	.antMatchers("/**").permitAll()
//                .and()
//            .formLogin()
//                .loginPage("/accedi")
//                .defaultSuccessUrl("/");
//        // when authentication attempt fails, redirect the browser to /accedi?error (since we have not specified otherwise)
//        // when we successfully logout, redirect the browser to /accedi?logout (since we have not specified otherwise)
//        
//    }

//
//  @Bean
//  @Override
//  public AuthenticationManager authenticationManagerBean() throws Exception {
//       return super.authenticationManagerBean();
//  }


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
