package net.yadaframework.components;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaLocalePathChangeInterceptor;

/**
 * Locale in the path
 * See https://stackoverflow.com/a/23847484/587641
 * This must be added to the WebApplicationInitializer.getServletFilters() method in each webapp (see yada docs)
 */
@Component
public class YadaLocalePathVariableFilter implements Filter {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	private boolean enabled = false;
	
	@Autowired private YadaConfiguration config;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		String url = request.getRequestURI().substring(request.getContextPath().length());
		if (!config.isLocalePathVariableEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}
	    String[] variables = url.split("/", 3);

	    if (variables.length > 1 && isLocale(variables[1])) {
	        request.setAttribute(YadaLocalePathChangeInterceptor.LOCALE_ATTRIBUTE_NAME, variables[1]);
	        String newUrl = StringUtils.removeStart(url, '/' + variables[1]);
	        RequestDispatcher dispatcher = request.getRequestDispatcher(newUrl);
	        dispatcher.forward(request, response);
	    } else {
	        filterChain.doFilter(request, response);
	    }
	}

	private boolean isLocale(String language) {
	    if (StringUtils.trimToNull(language)==null) {
	    	return false;
	    }
	    List<String> languages = config.getLanguages();
	    return languages.contains(language);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// Empty
		// This is not called anyway, when using Spring Security
	}

	@Override
	public void destroy() {
		// Empty
	}

	public boolean isEnabled() {
		return enabled;
	}
}
