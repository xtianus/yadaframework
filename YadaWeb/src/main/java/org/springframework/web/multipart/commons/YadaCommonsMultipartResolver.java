package org.springframework.web.multipart.commons;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

// Nota: forse in locale non funziona ma in remoto pare di sì
public class YadaCommonsMultipartResolver extends CommonsMultipartResolver {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());
	private static final String REQUEST_ATTRIBUTE = "MaxUploadSizeExceededException";
	
	public static boolean limitExceeded(HttpServletRequest request) {
		return request.getAttribute(REQUEST_ATTRIBUTE)!=null;
	}
	
	@Override
	protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
		try {
			return super.parseRequest(request);
		} catch (MaxUploadSizeExceededException e) {
			log.debug(REQUEST_ATTRIBUTE, e);
			request.setAttribute(REQUEST_ATTRIBUTE, e);
			return parseFileItems(Collections.<FileItem> emptyList(), null);
		}
	}
}
