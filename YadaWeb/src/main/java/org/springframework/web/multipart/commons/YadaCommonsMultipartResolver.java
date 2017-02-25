package org.springframework.web.multipart.commons;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

// Nota: forse in locale non funziona ma in remoto pare di sì
public class YadaCommonsMultipartResolver extends CommonsMultipartResolver {
	@Override
	protected MultipartParsingResult parseRequest(HttpServletRequest request) throws MultipartException {
		try {
			return super.parseRequest(request);
		} catch (MaxUploadSizeExceededException e) {
			request.setAttribute("MaxUploadSizeExceededException", e);
			return parseFileItems(Collections.<FileItem> emptyList(), null);
		}
	}
}
