package net.yadaframework.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaLogRedaction;

/**
 * Answers 413 (Payload Too Large) instead of 403 when CSRF validation fails on a multipart request whose body
 * exceeded the upload limit. The multipart body is parsed before Spring Security by the MultipartFilter: when it is too
 * large, the CSRF form field is never read, so a plain form can't be validated and is rejected before reaching any
 * controller. Requests that send the CSRF token in a header pass validation and reach the controller, where
 * {@link YadaCommonsMultipartResolver#limitExceeded(HttpServletRequest)} is true; an invalid header token gets the
 * normal CSRF failure. Any other access denial is handled by the delegate.
 */
public class YadaUploadLimitAccessDeniedHandler implements AccessDeniedHandler {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private final AccessDeniedHandler delegate;

	/**
	 * @param delegate the handler for every access denial that is not caused by an oversized multipart body
	 */
	public YadaUploadLimitAccessDeniedHandler(AccessDeniedHandler delegate) {
		this.delegate = delegate;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
		if (accessDeniedException instanceof CsrfException && YadaCommonsMultipartResolver.limitExceeded(request) && !hasCsrfHeader(request) && !response.isCommitted()) {
			log.debug("Rejecting oversized multipart request to {} that has no readable CSRF token", YadaLogRedaction.redactTokens(request.getRequestURI()));
			response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
			return;
		}
		delegate.handle(request, response, accessDeniedException);
	}

	/**
	 * A token sent in the header could be read and was invalid, so it's a normal CSRF failure
	 */
	private boolean hasCsrfHeader(HttpServletRequest request) {
		CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
		return csrfToken != null && request.getHeader(csrfToken.getHeaderName()) != null;
	}
}
