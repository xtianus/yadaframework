package net.yadaframework.tests.web.security.auth;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaNotify;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.web.YadaRegistrationController;
import net.yadaframework.security.web.YadaRegistrationController.YadaChangeUsernameOutcome;

/**
 * Email change scenario of the authentication tests: a logged-in user asks for a new email, which is applied when the
 * link of the confirmation email is opened. The framework has no endpoint for this flow, so it is wired here as an
 * application would do with YadaRegistrationController.
 */
@Controller
public class EmailChangeScenarioController {
	private static final String OUTCOME_PAGE = "redirect:/test/home";

	private final YadaRegistrationController yadaRegistrationController;
	private final YadaSecurityUtil yadaSecurityUtil;
	private final YadaUserCredentialsDao yadaUserCredentialsDao;
	private final YadaNotify yadaNotify;

	public EmailChangeScenarioController(YadaRegistrationController yadaRegistrationController, YadaSecurityUtil yadaSecurityUtil, YadaUserCredentialsDao yadaUserCredentialsDao, YadaNotify yadaNotify) {
		this.yadaRegistrationController = yadaRegistrationController;
		this.yadaSecurityUtil = yadaSecurityUtil;
		this.yadaUserCredentialsDao = yadaUserCredentialsDao;
		this.yadaNotify = yadaNotify;
	}

	/**
	 * Sends the confirmation email for a new email of the logged-in user
	 */
	@PostMapping("/test/security/auth/emailChange")
	public String emailChange(String newEmail, HttpServletRequest request, Locale locale, RedirectAttributes redirectAttributes) {
		String username = yadaSecurityUtil.getUsername();
		if (username == null) {
			return "redirect:/login";
		}
		YadaUserCredentials yadaUserCredentials = yadaUserCredentialsDao.findFirstByUsername(username);
		boolean sent = yadaRegistrationController.handleEmailChangeRequest(yadaUserCredentials, newEmail, request, locale);
		if (sent) {
			yadaNotify.titleKey(redirectAttributes, locale, "yts.emailChange.title").ok().messageKey("yts.emailChange.sent").add();
		} else {
			yadaNotify.titleKey(redirectAttributes, locale, "yts.emailChange.title").error().messageKey("yts.emailChange.failed").add();
		}
		return OUTCOME_PAGE;
	}

	/**
	 * The link of the email change confirmation email
	 */
	@GetMapping("/changeEmailConfirm/{token}")
	public String changeEmailConfirm(@PathVariable String token, Locale locale, RedirectAttributes redirectAttributes) {
		YadaChangeUsernameOutcome outcome = yadaRegistrationController.changeUsername(token);
		switch (outcome.resultCode) {
		case OK -> yadaNotify.titleKey(redirectAttributes, locale, "yts.emailChange.title").ok().messageKey("yts.emailChange.done").add();
		case LINK_EXPIRED, REQUEST_INVALID -> yadaNotify.titleKey(redirectAttributes, locale, "yts.emailChange.title").error().messageKey("yts.emailChange.expired").add();
		case USER_EXISTS, ERROR -> yadaNotify.titleKey(redirectAttributes, locale, "yts.emailChange.title").error().messageKey("yts.emailChange.failed").add();
		}
		return OUTCOME_PAGE;
	}
}
