package net.yadaframework.tests.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import net.yadaframework.tests.components.TestDataService;

/**
 * Small pages that exercise real framework behavior for the integration and browser suites.
 */
@Controller
@RequestMapping("/test")
public class ScenarioController {
	private final TestDataService testDataService;

	public ScenarioController(TestDataService testDataService) {
		this.testDataService = testDataService;
	}

	@GetMapping("/home")
	public String home(Model model) {
		model.addAttribute("adminUser", testDataService.findUser(TestDataService.ADMIN_EMAIL));
		return "/test/home";
	}

	@GetMapping("/user/area")
	public String userArea() {
		return "/test/userArea";
	}

	@GetMapping("/admin/area")
	public String adminArea() {
		return "/test/adminArea";
	}

	/**
	 * The error page, reached by forward from YadaController.yadaError() with the original HTTP status
	 */
	@RequestMapping("/error")
	public String error() {
		return "/test/error";
	}

	/**
	 * A state-changing request that Spring Security only accepts with a valid CSRF token
	 */
	@PostMapping("/echo")
	@ResponseBody
	public String echo(String message) {
		return "ECHO:" + message;
	}
}
