package net.yadaframework.tests.web;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import jakarta.servlet.http.HttpServletRequest;

import net.yadaframework.security.YadaSecurityConfig;

/**
 * The home and login entry points that YadaSecurityConfig expects from every application.
 */
@Controller
public class HomeController {

	/**
	 * Target of the login and logout success handlers and of the framework redirects to "/".
	 * Flash attributes, like the notifications of a previous redirect, are passed on to the home page.
	 */
	@RequestMapping("/")
	public String home(HttpServletRequest request, RedirectAttributes redirectAttributes) {
		Map<String, ?> inputFlashMap = RequestContextUtils.getInputFlashMap(request);
		if (inputFlashMap != null) {
			inputFlashMap.forEach(redirectAttributes::addFlashAttribute);
		}
		return "redirect:/test/home";
	}

	/**
	 * The login page, shown on request and forwarded to by the authentication failure handler,
	 * which sets the error flags as request attributes
	 */
	@RequestMapping(YadaSecurityConfig.DEFAULT_LOGIN_URL)
	public String login() {
		return "/test/login";
	}
}
