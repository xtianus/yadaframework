package org.springframework.web.multipart.commons;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

// Nota: forse in locale non funziona ma in remoto pare di sì
public class YadaCommonsMultipartResolver extends StandardServletMultipartResolver {
	private final Logger log = LoggerFactory.getLogger(getClass());
	public static final String MAX_UPLOAD_SIZE_EXCEEDED_KEY = "MaxUploadSizeExceededException";

	@Override
	public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
		try {
			return super.resolveMultipart(request);
		} catch (MaxUploadSizeExceededException e) {
			request.setAttribute(MAX_UPLOAD_SIZE_EXCEEDED_KEY, e);
			log.debug("Max upload file size exceeded", e);
			return new StandardMultipartHttpServletRequest(request, true);
		}
	}

	/**
	 * Check if the file upload of the current request has exceeded the configured limit.
	 *
	 * @param request
	 * @return
	 */
	public static boolean limitExceeded(HttpServletRequest request) {
		return request.getAttribute(MAX_UPLOAD_SIZE_EXCEEDED_KEY) != null;
	}

}
