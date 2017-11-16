package org.springframework.web.multipart.commons;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

// Nota: forse in locale non funziona ma in remoto pare di sì
public class YadaCommonsMultipartResolver extends CommonsMultipartResolver {
	public static final String MAX_UPLOAD_SIZE_EXCEEDED_KEY = "MaxUploadSizeExceededException";

	@Override
	protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
		try {
			return super.parseRequest(request);
		} catch (MaxUploadSizeExceededException e) {
			request.setAttribute(MAX_UPLOAD_SIZE_EXCEEDED_KEY, e);
			return parseFileItems(Collections.<FileItem> emptyList(), null);
		}
	}

	/**
	 * Check if the file upload of the current request has exceeded the configured limit.
	 * @param request
	 * @return
	 */
	public static boolean limitExceeded(HttpServletRequest request) {
		return request.getAttribute(MAX_UPLOAD_SIZE_EXCEEDED_KEY)!=null;
	}
	
	/**
	 * Returns the file upload limit exception if any
	 * @param request
	 * @return the exception, or null if no exception occurred
	 */
	public static MaxUploadSizeExceededException limitException(HttpServletRequest request) {
		return (MaxUploadSizeExceededException) request.getAttribute(MAX_UPLOAD_SIZE_EXCEEDED_KEY);
	}
}
