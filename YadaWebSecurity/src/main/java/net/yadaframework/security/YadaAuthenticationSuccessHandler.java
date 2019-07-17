package net.yadaframework.security;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsRepository;

// Si può inserire il codice da eseguire dopo un login che ha avuto successo
@Component
@Scope("prototype") // In case you have more than one YadaSecurityConfig bean
public class YadaAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private String defaultTargetUrlAjaxRequest = "/";
	private String defaultTargetUrlNormalRequest = "/";

	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaUserCredentialsRepository userCredentialsRepository;

	private final static String UNSET_TARGET_URL = "/YADA_UNSET_TARGET_URL"; // Can't just use null because it's rejected

	public YadaAuthenticationSuccessHandler() {
		// Set so that we know when to return our saved value in determineTargetUrl()
		super.setDefaultTargetUrl(UNSET_TARGET_URL);
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
		String username = authentication.getName();
		userCredentialsRepository.updateLoginTimestamp(username.toLowerCase());
		userCredentialsRepository.resetFailedAttempts(username.toLowerCase());
		super.onAuthenticationSuccess(request, response, authentication);
	}

	public String getDefaultTargetUrlAjaxRequest() {
		return defaultTargetUrlAjaxRequest;
	}

	/**
	 * Target url to redirect after login when the request is Ajax
	 */
	public void setDefaultTargetUrlAjaxRequest(String defaultTargetUrlAjaxRequest) {
		this.defaultTargetUrlAjaxRequest = defaultTargetUrlAjaxRequest;
	}

	public String getDefaultTargetUrlNormalRequest() {
		return defaultTargetUrlNormalRequest;
	}

	/**
	 * Target url to redirect after login when the request is not Ajax
	 */
	public void setDefaultTargetUrlNormalRequest(String defaultTargetUrlNormalRequest) {
		this.defaultTargetUrlNormalRequest = defaultTargetUrlNormalRequest;
	}

	/**
	 * Thread-safe way of changing the default target url based on the request type (ajax/normal)
	 */
	@Override
	protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response) {
		// We enter here only when there is no saved request, so a login form has been submitted "on purpose" and not because
		// the user requested a protected page
		String targetUrl = super.determineTargetUrl(request, response);
		if (!UNSET_TARGET_URL.equals(targetUrl)) {
			// The target url was determined either by a request parameter or a request "Referer" header
			log.debug("Login target url from either request parameter or Referer header: ", targetUrl);
			return targetUrl;
		}
		// We can return either the ajax default url or the normal default url
		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
			targetUrl = defaultTargetUrlAjaxRequest;
		} else {
			targetUrl = defaultTargetUrlNormalRequest;
		}
		if (yadaConfiguration.isLocalePathVariableEnabled()) {
			Locale locale = LocaleContextHolder.getLocale();
			if (locale!=null) {
				targetUrl = "/" + locale.getLanguage() + targetUrl;
			}
		}
		return targetUrl;
	}

}
