package net.yadaframework.security;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
// import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.components.YadaAuthenticationFailureHandler;
import net.yadaframework.security.components.YadaAuthenticationSuccessHandler;
import net.yadaframework.security.components.YadaLogoutSuccessHandler;
import net.yadaframework.security.components.YadaUserDetailsService;

/**
 * Basic security configuration.
 * The application paths must be protected in an application-specific subclass of WebSecurityConfigurerAdapter
 */
// @Configuration not needed because set in the subclass?
@EnableWebSecurity
@ComponentScan(basePackages = { "net.yadaframework.security.components", "net.yadaframework.security.persistence.repository" })
@Order(10) // Just in case there will be others
public class YadaSecurityConfig {
//	private Logger log = LoggerFactory.getLogger(YadaSecurityConfig.class);

	@Autowired private YadaUserDetailsService userDetailsService;
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaWebUtil yadaWebUtil;

	@Autowired protected YadaAuthenticationFailureHandler failureHandler;
	@Autowired protected YadaAuthenticationSuccessHandler successHandler;
	@Autowired protected YadaLogoutSuccessHandler logoutSuccessHandler;
	@Autowired protected PasswordEncoder passwordEncoder;

	// This should be used by any @RequestMapping that wants to open the login page/modal
	public final static String DEFAULT_LOGIN_URL = "/login";
	public final static String DEFAULT_LOGIN_URL_AJAX = "/ajaxLogin";
	public final static String DEFAULT_LOGIN_POST = "/loginPost";
	
	// These can be overridden
	protected String loginUrl = DEFAULT_LOGIN_URL;
	protected String loginUrlAjax = DEFAULT_LOGIN_URL_AJAX;
	protected String loginPost = DEFAULT_LOGIN_POST;

	/**
	 * Configures basic security settings. Must be overridden to configure url protections.
	 */
	protected void configure(HttpSecurity http) throws Exception {
		failureHandler.setFailureUrlAjaxRequest(loginUrlAjax);
		failureHandler.setFailureUrlNormalRequest(loginUrl);
		// The "/yadaLoginSuccess" target can be overridden to include the redirect to any target page.
		// See YadaLoginController.yadaLoginSuccess()
		successHandler.setDefaultTargetUrlAjaxRequest("/yadaLoginSuccess"); // Returns the string "success"
		successHandler.setDefaultTargetUrlNormalRequest("/");
		logoutSuccessHandler.setDefaultTargetUrl("/"); // language path will be added in the handler

	    http
	        .headers(headers -> headers.disable())
	        .csrf(csrf -> csrf.disable())
	        .sessionManagement(sessions -> sessions.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
	        .logout(logout -> {
	            logout
	                .logoutUrl("/logout")
	                .logoutSuccessHandler(logoutSuccessHandler);
	            // .logoutSuccessUrl("/") // TODO rimanere nella pagina corrente se non è protetta!
	            // .invalidateHttpSession(false) // Lascio che la session si cancelli quando esco
	        })
	        .formLogin(formLogin -> formLogin
				.loginPage(loginUrl) // url of the login form (GET)
				.loginProcessingUrl(loginPost) // url where the login form is sent (POST)
				.failureHandler(failureHandler)
				.successHandler(successHandler))
	        .exceptionHandling(exceptionHandling -> {
	            // This is needed to redirect to a language-specific login url
	            exceptionHandling.authenticationEntryPoint(new CustomAuthenticationEntryPoint());
	        })
	        .requestCache(requestCache -> {
	            if (yadaConfiguration.isLocalePathVariableEnabled()) {
	                // Resetto la RequestCache in modo che salvi le request di qualunque tipo, anche ajax,
	                // altrimenti il meccanismo del redirect alla pagina di partenza non funziona con le chiamate ajax.
	                requestCache.requestCache(new YadaLocalePathRequestCache());
	            } else {
	                requestCache.requestCache(new HttpSessionRequestCache());
	            }
	        })
	        .authorizeHttpRequests(authorize -> {
	            // Forward requests should never be protected with Spring MVC: https://docs.spring.io/spring-security/reference/5.8/migration/servlet/authorization.html#_permit_forward_when_using_spring_mvc
	            // This is especially the case when using YadaLocalePathVariableFilter.
	            authorize
	                .dispatcherTypeMatchers(DispatcherType.FORWARD).permitAll();
	        });
	}


	/**
	 * Needed to redirect to a language-specific login url when a protected page is requested
	 */
    public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        	boolean ajaxRequest = yadaWebUtil.isAjaxRequest(request);
        	String fixedLoginUrl = ajaxRequest?loginUrlAjax:loginUrl;
        	if (yadaConfiguration.isLocalePathVariableEnabled()) {
        		Locale locale = LocaleContextHolder.getLocale();
        		fixedLoginUrl = yadaWebUtil.enhanceUrl(fixedLoginUrl, locale);
        	}
            response.sendRedirect(fixedLoginUrl);
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
					processor.setPasswordEncoder(passwordEncoder);
				}
				return processor;
			}
		});
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
			if(allowedMethods.matcher(request.getMethod()).matches()) {
				return false;
			}

			// No CSRF due to api call
			if(apiMatcher.matches(request)) {
				return false;
			}

			// CSRF for everything else that is not an API call or an allowedMethod
			return true;
		}
	}

	@Bean(name="filterMultipartResolver")
	public MultipartResolver multipartResolver() {
		return new YadaCommonsMultipartResolver();
	}
	
	@Bean
	// This is used by YadaSecurityUtil.checkUrlAccess()
	public List<AuthorizationManager<HttpServletRequest>> authorizationManagers(List<SecurityFilterChain> filterChains) {
	    return filterChains.stream()
	        .map(filterChain -> {
	            AuthorizationFilter filter = (AuthorizationFilter) filterChain.getFilters().stream()
	                .filter(f -> f instanceof AuthorizationFilter).findFirst()
	                .orElse(null);
	            return filter != null ? filter.getAuthorizationManager() : null;
	        })
	        .filter(manager -> manager != null)
	        .collect(java.util.stream.Collectors.toList());
	}
}
