package net.yadaframework.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import net.yadaframework.components.YadaLocalePathVariableFilter;

/**
 * This is needed to store the original request on access failure so that after login the browser is redirected to 
 * the url with locale in path. Otherwise the locale would be lost after redirect.
 */
public class YadaLocalePathRequestCache extends HttpSessionRequestCache {

	@Override
	public void saveRequest(HttpServletRequest request, HttpServletResponse response) {
		Object originalRequest = request.getAttribute(YadaLocalePathVariableFilter.ORIGINAL_REQUEST);
		if (originalRequest!=null) {
			request = (HttpServletRequest) originalRequest;
		}
		super.saveRequest(request, response);
	}


}
