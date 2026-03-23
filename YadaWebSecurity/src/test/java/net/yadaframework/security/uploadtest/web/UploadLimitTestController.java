package net.yadaframework.security.uploadtest.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Endpoints used by the upload limit integration tests.
 */
@Controller
public class UploadLimitTestController {

	/**
	 * Renders the upload form page.
	 * @return the form view
	 */
	@GetMapping("/test/upload/form")
	public String form() {
		return "/test/upload/form";
	}

	/**
	 * Receives the uploaded file and reports when the multipart limit has been exceeded.
	 * @param upfile uploaded file, null when parsing failed
	 * @param request current request
	 * @return explicit status marker for the integration test
	 */
	@PostMapping("/test/upload/submit")
	@ResponseBody
	public ResponseEntity<String> submit(MultipartFile upfile, HttpServletRequest request) {
		if (YadaCommonsMultipartResolver.limitExceeded(request)) {
			return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("LIMIT_EXCEEDED");
		}
		return ResponseEntity.ok(upfile == null ? "UPLOAD_OK" : "UPLOAD_OK");
	}
}
