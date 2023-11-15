package net.yadaframework.security.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.TooManyFailedAttemptsException;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;

/**
 * Questa classe aggiunge un pò di informazioni in request quando il login fallisce.
 *
 *
 */
@Component
@Scope("prototype") // In case you have more than one YadaSecurityConfig bean
public class YadaAuthenticationFailureHandler implements AuthenticationFailureHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	public final static String REQUESTATTR_LOGINERRORFLAG="loginErrorFlag"; // Set when login fails
	// Failure reason flags
	public final static String REQUESTATTR_PASSWORDERRORFLAG="passwordError";
	public final static String REQUESTATTR_USERDISABLEDFLAG="userDisabled";
	public final static String REQUESTATTR_USERNAMENOTFOUNDFLAG="usernameNotFound";
	public final static String REQUESTATTR_CREDENTIALSEXPIREDFLAG="credentialsExpiredException";
	public final static String REQUESTATTR_GENERICERRORFLAG="loginError";
	// Login data
	public final static String REQUESTATTR_USERNAME="username";
	public final static String REQUESTATTR_PASSWORD="password";
	//
	public final static String REQUESTATTR_LOCKOUTMINUTES="lockoutMinutes";

	private String failureUrlAjaxRequest = null;
	private String failureUrlNormalRequest = null;

	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaConfiguration yadaConfiguration;

	public YadaAuthenticationFailureHandler() {
	}

	private static void addIfNotNull(List<String> params, String name, HttpServletRequest request) {
		// This method is used both when the flags are added to the request as attributes and when they are sent as parameters
		Object value = request.getAttribute(name);
		if (value==null) {
			value = request.getParameter(name);
		}
		if (value!=null) {
			params.add(name);
			params.add((String) value);
		}
	}

	/**
	 * Returns a sequential list of name and value pairs for the login error parameters/attributes that are not null
	 * @param request
	 * @return
	 */
	public static List<String> getLoginErrorParams(HttpServletRequest request) {
		List<String> params = new ArrayList<>();
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_CREDENTIALSEXPIREDFLAG, request);
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_GENERICERRORFLAG, request);
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_LOCKOUTMINUTES, request);
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_LOGINERRORFLAG, request);
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_PASSWORDERRORFLAG, request);
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_USERDISABLEDFLAG, request);
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_USERNAME, request);
		addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_USERNAMENOTFOUNDFLAG, request);
		// Do not add the password value for security reasons
		// addIfNotNull(params, YadaAuthenticationFailureHandler.REQUESTATTR_PASSWORD, request);
		return params;
	}

	@Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		request.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception); // Lo faccio per compatibilità con li modo standard di gestire la cosa, ma non serve
		String username = request.getParameter("username");
		request.setAttribute(REQUESTATTR_USERNAME, username);
		request.setAttribute(REQUESTATTR_LOGINERRORFLAG, REQUESTATTR_LOGINERRORFLAG);
		try {
			if (exception instanceof BadCredentialsException) {
				request.setAttribute(REQUESTATTR_PASSWORDERRORFLAG, REQUESTATTR_PASSWORDERRORFLAG);
				yadaUserCredentialsDao.incrementFailedAttempts(username);
			} else if (exception instanceof DisabledException) {
				request.setAttribute(REQUESTATTR_USERDISABLEDFLAG, REQUESTATTR_USERDISABLEDFLAG);
			} else if (exception instanceof CredentialsExpiredException) {
				request.setAttribute(REQUESTATTR_CREDENTIALSEXPIREDFLAG, REQUESTATTR_CREDENTIALSEXPIREDFLAG);
				request.getRequestDispatcher("/pwdChange").forward(request, response);
				return;
			} else if (exception instanceof TooManyFailedAttemptsException) {
				YadaUserCredentials yadaUserCredentials = yadaUserCredentialsDao.findFirstByUsername(username.toLowerCase());
				if (yadaUserCredentials!=null) {
					int lockMinutes = yadaConfiguration.getPasswordFailedAttemptsLockoutMinutes();
					Date lastFailedTimestamp = yadaUserCredentials.getLastFailedAttempt();
					if (lastFailedTimestamp!=null) {
						long minutesPassed = (long) Math.ceil((System.currentTimeMillis() - lastFailedTimestamp.getTime()) / 60000);
						long minutesLeft = lockMinutes - minutesPassed;
						request.setAttribute(REQUESTATTR_LOCKOUTMINUTES, minutesLeft);
					}
				}
			} else if (exception instanceof UsernameNotFoundException) {
				// ATTENZIONE, deve essere l'ultima della catena di if altrimenti le sottoclassi non vengono considerate
				request.setAttribute(REQUESTATTR_USERNAMENOTFOUNDFLAG, REQUESTATTR_USERNAMENOTFOUNDFLAG);
				request.setAttribute(REQUESTATTR_PASSWORD, request.getParameter("password"));
			} else {
				request.setAttribute(REQUESTATTR_GENERICERRORFLAG, REQUESTATTR_GENERICERRORFLAG);
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

        	// This is a forward, so there's no need to prefix with the language as done in the success handler

//        	String requestLocaleString = (String) request.getAttribute(YadaLocalePathChangeInterceptor.LOCALE_ATTRIBUTE_NAME);
//        	if (requestLocaleString!=null) {
//        		Locale requestLocale = StringUtils.parseLocaleString(requestLocaleString);
//    	    	if (requestLocale!=null) {
//    	    		failureUrl = yadaWebUtil.enhanceUrl(failureUrl, requestLocale);
//    			}
//        	}

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
