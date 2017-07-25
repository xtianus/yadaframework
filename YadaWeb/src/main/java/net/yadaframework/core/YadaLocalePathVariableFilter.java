package net.yadaframework.core;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Locale in the path
 * See https://stackoverflow.com/a/23847484/587641
 */
public class YadaLocalePathVariableFilter extends OncePerRequestFilter {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
	    String url = request.getRequestURI().substring(request.getContextPath().length());
	    String[] variables = url.split("/", 3);

	    if (variables.length > 1 && isLocale(variables[1])) {
	    	log.debug("Found locale {}", variables[1]);
	        request.setAttribute(YadaLocalePathChangeInterceptor.LOCALE_ATTRIBUTE_NAME, variables[1]);
	        String newUrl = StringUtils.removeStart(url, '/' + variables[1]);
	        log.debug("Dispatching to new url \'{}\'", newUrl);
	        RequestDispatcher dispatcher = request.getRequestDispatcher(newUrl);
	        dispatcher.forward(request, response);
	    } else {
	        filterChain.doFilter(request, response);
	    }
	}

	private boolean isLocale(String locale) {
	    //validate the string here against an accepted list of locales or whatever
	    try {
	        LocaleUtils.toLocale(locale);
	        return true;
	    } catch (IllegalArgumentException e) {
	    	log.debug("Variable \'{}\' is not a Locale", locale);
	    }
	    return false;
	}
}
