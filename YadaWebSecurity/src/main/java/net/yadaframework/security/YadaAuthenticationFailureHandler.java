package net.yadaframework.security;

import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaLocalePathChangeInterceptor;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsRepository;
import net.yadaframework.web.YadaWebUtil;

/**
 * Questa classe aggiunge un pò di informazioni in request quando il login fallisce.
 * 
 *
 */
@Component
@Scope("prototype") // In case you have more than one YadaSecurityConfig bean
public class YadaAuthenticationFailureHandler implements AuthenticationFailureHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	private String failureUrlAjaxRequest = null;
	private String failureUrlNormalRequest = null;
	
	@Autowired private YadaUserCredentialsRepository userCredentialsRepository;
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaWebUtil yadaWebUtil;

	public YadaAuthenticationFailureHandler() {
	}

	@Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception); // Lo faccio per compatibilità con li modo standard di gestire la cosa, ma non serve
		String username = request.getParameter("username");
		request.setAttribute("username", username);
		request.setAttribute("loginErrorFlag", "loginErrorFlag");
		try {
			if (exception instanceof BadCredentialsException) {
				request.setAttribute("passwordError", "passwordError");
				userCredentialsRepository.incrementFailedAttempts(username);
			} else if (exception instanceof DisabledException) {
				request.setAttribute("userDisabled", "userDisabled");
			} else if (exception instanceof CredentialsExpiredException) {
				request.setAttribute("credentialsExpiredException", "credentialsExpiredException");
				request.getRequestDispatcher("/pwdChange").forward(request, response);
				return;
			} else if (exception instanceof TooManyFailedAttemptsException) {
				YadaUserCredentials yadaUserCredentials = userCredentialsRepository.findByUsername(username.toLowerCase(), new PageRequest(0, 1)).get(0);
				int lockMinutes = yadaConfiguration.getPasswordFailedAttemptsLockoutMinutes();
				Date lastFailedTimestamp = yadaUserCredentials.getLastFailedAttempt();
				if (lastFailedTimestamp!=null) {
					long minutesPassed = (long) Math.ceil((System.currentTimeMillis() - lastFailedTimestamp.getTime()) / 60000);
					long minutesLeft = lockMinutes - minutesPassed;
					request.setAttribute("lockoutMinutes", minutesLeft);
				}
			} else if (exception instanceof UsernameNotFoundException) { 
				// ATTENZIONE, deve essere l'ultima della catena di if altrimenti le sottoclassi non vengono considerate
				request.setAttribute("usernameNotFound", "usernameNotFound");
				request.setAttribute("password", request.getParameter("password"));
			} else {
				request.setAttribute("loginError", "loginError");
			}
		} catch (Exception e) {
			log.error("Failed to handle authentication failure (ignored)", e);
		}
		
		boolean ajaxRequest = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
		String failureUrl = ajaxRequest?failureUrlAjaxRequest:failureUrlNormalRequest;
        if (failureUrl == null) {
            log.debug("No failure URL set, sending 401 Unauthorized error");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed: " + exception.getMessage());
        } else {
        	log.debug("Forwarding to " + failureUrl);
        	
        	// During login for some reason YadaLocalePathChangeInterceptor is not called and we don't get the proper locale in the context
        	String requestLocaleString = (String) request.getAttribute(YadaLocalePathChangeInterceptor.LOCALE_ATTRIBUTE_NAME);
        	if (requestLocaleString!=null) {
        		Locale requestLocale = StringUtils.parseLocaleString(requestLocaleString);
    	    	if (requestLocale!=null) {
    	    		failureUrl = yadaWebUtil.enhanceUrl(failureUrl, requestLocale);
    			}
        	}
        	
            request.getRequestDispatcher(failureUrl).forward(request, response);
        }
    }

	public String getFailureUrlAjaxRequest() {
		return failureUrlAjaxRequest;
	}

	public void setFailureUrlAjaxRequest(String failureUrlAjaxRequest) {
		this.failureUrlAjaxRequest = failureUrlAjaxRequest;
	}

	public String getFailureUrlNormalRequest() {
		return failureUrlNormalRequest;
	}

	public void setFailureUrlNormalRequest(String failureUrlNormalRequest) {
		this.failureUrlNormalRequest = failureUrlNormalRequest;
	}

}
