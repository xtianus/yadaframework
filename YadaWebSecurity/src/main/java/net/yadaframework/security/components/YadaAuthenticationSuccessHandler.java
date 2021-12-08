package net.yadaframework.security.components;

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaConstants;
import net.yadaframework.core.YadaLocalePathChangeInterceptor;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.persistence.repository.YadaUserProfileDao;

// Si può inserire il codice da eseguire dopo un login che ha avuto successo
@Component
@Scope("prototype") // In case you have more than one YadaSecurityConfig bean
public class YadaAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private String defaultTargetUrlAjaxRequest = "/";
	private String defaultTargetUrlNormalRequest = "/";

	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaUserProfileDao yadaUserProfileDao;
	@Autowired private YadaWebUtil yadaWebUtil;

	private final static String UNSET_TARGET_URL = "/YADA_UNSET_TARGET_URL"; // Can't just use null because it's rejected

	public YadaAuthenticationSuccessHandler() {
		// Set so that we know when to return our saved value in determineTargetUrl()
		super.setDefaultTargetUrl(UNSET_TARGET_URL);
	}

	/**
	 * Custom code to be executed after login or autologin. Can be overridden.
	 * @param request
	 * @param authentication
	 */
	public void onAuthenticationSuccessCustom(HttpServletRequest request, Authentication authentication) {
		String username = authentication.getName().toLowerCase();
		yadaUserCredentialsDao.updateLoginTimestamp(username);
		yadaUserCredentialsDao.resetFailedAttempts(username);
		// Refresh the timezone too
		HttpSession session = request.getSession(false);
		if (session!=null) {
			TimeZone timezone = (TimeZone) session.getAttribute(YadaConstants.SESSION_USER_TIMEZONE);
			if (timezone!=null) {
				yadaUserProfileDao.updateTimezone(username, timezone);
			}
		}
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
		onAuthenticationSuccessCustom(request, authentication);
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
			// The target url must be prefixed by the language because it is used as a redirect
			//
			// During login for some reason YadaLocalePathChangeInterceptor is not called and we don't get the proper locale in the context
			String requestLocaleString = (String) request.getAttribute(YadaLocalePathChangeInterceptor.LOCALE_ATTRIBUTE_NAME);
			if (requestLocaleString!=null) {
				Locale requestLocale = StringUtils.parseLocaleString(requestLocaleString);
				if (requestLocale!=null) {
					targetUrl = yadaWebUtil.enhanceUrl(targetUrl, requestLocale);
				}
			}

		}
		return targetUrl;
	}

}
