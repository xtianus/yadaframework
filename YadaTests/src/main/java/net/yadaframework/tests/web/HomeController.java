package net.yadaframework.tests.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import net.yadaframework.security.YadaSecurityConfig;

/**
 * The home and login entry points that YadaSecurityConfig expects from every application.
 */
@Controller
public class HomeController {

	/**
	 * Target of the login and logout success handlers
	 */
	@RequestMapping("/")
	public String home() {
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
