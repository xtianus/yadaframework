package net.yadaframework.security.uploadtest.web;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.core.YadaConfiguration;

/**
 * Endpoints used by the upload limit integration tests.
 */
@Controller
public class UploadLimitTestController {
	/** Number of uploads accepted by the application: the observable "mutation" of the tests */
	private final AtomicInteger acceptedUploads = new AtomicInteger();
	private final YadaConfiguration config;

	public UploadLimitTestController(YadaConfiguration config) {
		this.config = config;
	}

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
		acceptedUploads.incrementAndGet();
		return ResponseEntity.ok("UPLOAD_OK " + (upfile == null ? 0 : upfile.getSize()));
	}

	/**
	 * Receives an upload and reports how many multipart temporary files exist in the uploads folder while the request
	 * is being handled, so that a test can tell that the multipart body was stored on disk
	 * @param upfile uploaded file
	 * @return the number of files in the uploads folder
	 */
	@PostMapping("/test/upload/submitObservingTempFiles")
	@ResponseBody
	public String submitObservingTempFiles(MultipartFile upfile) {
		String[] files = config.getUploadsFolder().list();
		return String.valueOf(files == null ? 0 : files.length);
	}

	/**
	 * @return the number of uploads accepted so far
	 */
	@GetMapping("/test/upload/accepted")
	@ResponseBody
	public String accepted() {
		return String.valueOf(acceptedUploads.get());
	}
}
