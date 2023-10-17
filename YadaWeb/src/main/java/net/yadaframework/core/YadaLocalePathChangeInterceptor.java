package net.yadaframework.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Locale in the path
 * See https://stackoverflow.com/a/23847484/587641
 */
public class YadaLocalePathChangeInterceptor implements HandlerInterceptor {
	public static final String LOCALE_ATTRIBUTE_NAME = YadaLocalePathChangeInterceptor.class.getName() + ".LOCALE";

	/**
	 * Check if the current locale has been set on the url, e.g. /en/something
	 * @param request
	 * @return true if the locale was set using a path variable
	 */
	public static boolean localePathRequested(HttpServletRequest request) {
		return request.getAttribute(LOCALE_ATTRIBUTE_NAME)!=null;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
	    Object newLocale = request.getAttribute(LOCALE_ATTRIBUTE_NAME);
	    if (newLocale != null) {
	        LocaleResolver localeResolver = RequestContextUtils.getLocaleResolver(request);
	        if (localeResolver == null) {
	            throw new IllegalStateException("No LocaleResolver found: not in a DispatcherServlet request?");
	        }
	        localeResolver.setLocale(request, response, StringUtils.parseLocaleString(newLocale.toString()));
	    }
	    return true;
	}
}