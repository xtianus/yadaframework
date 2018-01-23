package net.yadaframework.security;

import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import net.yadaframework.core.YadaConfiguration;

/**
 * Basic security configuration. 
 * The application paths must be protected in an application-specific subclass of WebSecurityConfigurerAdapter
 */
@EnableWebSecurity
@Configuration
@EnableJpaRepositories(basePackages = "net.yadaframework.security.persistence.repository")
@ComponentScan(basePackages = { "net.yadaframework.security" })
@Order(10) // Just in case there will be others
public class YadaSecurityConfig extends WebSecurityConfigurerAdapter {
//	private Logger log = LoggerFactory.getLogger(YadaSecurityConfig.class);

	@Autowired private YadaUserDetailsService userDetailsService;
	@Autowired private YadaConfiguration yadaConfiguration;

	@Autowired protected YadaAuthenticationFailureHandler failureHandler;
	@Autowired protected YadaAuthenticationSuccessHandler successHandler;
	
	/**
	 * Configures basic security settings. Must be overridden to configure url protections.
	 */
	public void configure(HttpSecurity http) throws Exception {
		failureHandler.setFailureUrlAjaxRequest("/ajaxLoginForm"); 
		failureHandler.setFailureUrlNormalRequest("/login"); 
		successHandler.setDefaultTargetUrlAjaxRequest("/ajaxLoginOk"); // Returns the string "loginSuccess"
		successHandler.setDefaultTargetUrlNormalRequest("/");
		
		http
			.headers().disable()
			// http.antMatcher("/**/css/**").headers().disable();
			.csrf().disable()
	        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED).and()
		    .logout()
		    .logoutUrl("/logout") // POST con il CSRF attivo, GET altrimenti
		    	.logoutSuccessUrl("/") // TODO rimanere nella pagina corrente se non è protetta!
		    	// .invalidateHttpSession(false) // Lascio che la session si cancelli quando esco
		    	.and()
		    .formLogin()
		        .loginPage("/login") // url del form di login (GET)
		        .loginProcessingUrl("/loginPost") // url dove postare il form (POST)
		        .failureHandler(failureHandler)
		        .successHandler(successHandler);
	//        .defaultSuccessUrl("/");
	//    	.and()
	//    .apply(new SpringSocialConfigurer());        // .requireCsrfProtectionMatcher(new MyRequestMatcher());
	        	//	new NegatedRequestMatcher(new AntPathRequestMatcher("/ajaxStoryBunch", null)));
		
		// Resetto la RequestCache in modo che salvi le request di qualunque tipo, anche ajax, 
		// altrimenti il meccanismo del redirect alla pagina di partenza non funziona con le chiamate ajax.
		// Questo sarebbe il filtro impostato senza il reset, configurato in RequestCacheConfigurer:
		// AndRequestMatcher [requestMatchers=[NegatedRequestMatcher [requestMatcher=Ant [pattern='/**/favicon.ico']], NegatedRequestMatcher [requestMatcher=MediaTypeRequestMatcher [contentNegotiationStrategy=org.springframework.web.accept.HeaderContentNegotiationStrategy@16f239b, matchingMediaTypes=[application/json], useEquals=false, ignoredMediaTypes=[*/*]]], NegatedRequestMatcher [requestMatcher=RequestHeaderRequestMatcher [expectedHeaderName=X-Requested-With, expectedHeaderValue=XMLHttpRequest]]]]
		if (yadaConfiguration.isLocalePathVariableEnabled()) {
			http.requestCache().requestCache(new YadaLocalePathRequestCache());
		} else {
			http.requestCache().requestCache(new HttpSessionRequestCache());
		}
		
		if (yadaConfiguration.isLocalePathVariableEnabled()) {
			// Needed since we intercept FORWARDed requests because of the YadaLocalePathVariableFilter 
			http.authorizeRequests().filterSecurityInterceptorOncePerRequest(true);
		}
	}

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
	
    // Non più usato perchè CSRF disabilitato per tutti, visto che causa troppi 403 al timeout di session
    //
    // Questo consente di fare delle richieste ajax in post verso le url indicate, senza incorrere in un 403 Forbidden
    // Veniva usato per /ajaxStoryBunch ma adesso quello usa una GET quindi non serve più per ora.
    private static class MyRequestMatcher implements RequestMatcher {
        private Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");
        private AntPathRequestMatcher apiMatcher = new AntPathRequestMatcher("/ajaxStoryBunch*", null);
        // Per usare altri matcher si possono concatenare con AndRequestMatcher e simili
        // Vedi http://docs.spring.io/spring-security/site/docs/3.2.x/apidocs/ per i matcher disponibili
     // private RegexRequestMatcher apiMatcher = new RegexRequestMatcher("/ajax.*", null);

        @Override
        public boolean matches(HttpServletRequest request) {
            // No CSRF due to allowedMethod
            if(allowedMethods.matcher(request.getMethod()).matches())
                return false;

            // No CSRF due to api call
            if(apiMatcher.matches(request))
                return false;

            // CSRF for everything else that is not an API call or an allowedMethod
            return true; 
        }
    }

}
