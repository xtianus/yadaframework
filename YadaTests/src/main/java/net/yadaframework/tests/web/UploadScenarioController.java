package net.yadaframework.tests.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.YadaCommonsMultipartResolver;

import jakarta.servlet.http.HttpServletRequest;

/**
 * File upload within and above the configured maxFileUploadSizeBytes
 */
@Controller
@RequestMapping("/test/upload")
public class UploadScenarioController {

	@GetMapping("")
	public String form() {
		return "/test/upload";
	}

	@PostMapping("")
	public String upload(MultipartFile upfile, HttpServletRequest request, Model model) {
		if (YadaCommonsMultipartResolver.limitExceeded(request)) {
			model.addAttribute("tooLarge", true);
		} else if (upfile != null && !upfile.isEmpty()) {
			model.addAttribute("uploadedName", upfile.getOriginalFilename());
			model.addAttribute("uploadedSize", upfile.getSize());
		}
		return "/test/upload";
	}
}
