package org.springframework.web.multipart.commons;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// Nota: forse in locale non funziona ma in remoto pare di sì
public class YadaCommonsMultipartResolver {
	private final Logger log = LoggerFactory.getLogger(getClass());
	public static final String MAX_UPLOAD_SIZE_EXCEEDED_KEY = "MaxUploadSizeExceededException";

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
