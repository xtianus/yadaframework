package net.yadaframework.tests.web.security.headers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import net.yadaframework.security.YadaSecurityConfig;

/**
 * Target of the Ajax login failure forward of YadaAuthenticationFailureHandler, so that the request attributes
 * it sets can be inspected over HTTP
 */
@Controller
public class AjaxLoginScenarioController {

	@RequestMapping(YadaSecurityConfig.DEFAULT_LOGIN_URL_AJAX)
	public String ajaxLogin() {
		return "/test/security/headers/ajaxLogin";
	}
}
