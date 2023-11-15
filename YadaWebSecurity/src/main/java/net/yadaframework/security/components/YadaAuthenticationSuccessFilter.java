package net.yadaframework.security.components;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Converts the AJAX_LOGGEDIN_PARAM request parameter into a AJAX_LOGGEDIN_HEADER response header
 * so that the ajax target, after login, knows it has to close the login modal somehow (with a reload)
 *
 */
public class YadaAuthenticationSuccessFilter implements Filter {
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}
	@Override
	public void destroy() {}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		if (request.getParameter(YadaAuthenticationSuccessHandler.AJAX_LOGGEDIN_PARAM)!=null) {
			response.setHeader(YadaAuthenticationSuccessHandler.AJAX_LOGGEDIN_HEADER, "true");
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}
}