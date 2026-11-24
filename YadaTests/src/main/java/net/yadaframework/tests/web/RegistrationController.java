package net.yadaframework.tests.web;

import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.components.YadaNotify;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.web.YadaPasswordResetForm;
import net.yadaframework.security.web.YadaRegistrationController;
import net.yadaframework.security.web.YadaRegistrationController.YadaRegistrationOutcome;
import net.yadaframework.tests.persistence.entity.UserProfile;

/**
 * The registration and password recovery pages of the YadaTests application, wired to YadaRegistrationController as a
 * downstream application does (see the YadaExamples RegistrationController). The framework itself handles
 * "/yadaPasswordResetPost", "/passwordChangeAfterRequest" and "/autologin/{tokenLink}"; login is in {@link HomeController}.
 * The links sent by email are read from the CapturingMailSender in tests. The registration form binds only the fields
 * allowed by {@link YadaRegistrationController#setRegistrationAllowedFields(WebDataBinder, String...)}.
 */
@Controller
public class RegistrationController {
	/** The role given to users that complete the registration */
	public static final String REGISTERED_ROLE = "USER";
	/** Where the outcome notifications are shown */
	private static final String OUTCOME_PAGE = "redirect:/test/home";

	private final YadaRegistrationController yadaRegistrationController;
	private final YadaNotify yadaNotify;

	public RegistrationController(YadaRegistrationController yadaRegistrationController, YadaNotify yadaNotify) {
		this.yadaRegistrationController = yadaRegistrationController;
		this.yadaNotify = yadaNotify;
	}

	/**
	 * Only email, password and confirmPassword of the registration request can be bound
	 */
	@InitBinder("yadaRegistrationRequest")
	void registrationBinder(WebDataBinder binder) {
		YadaRegistrationController.setRegistrationAllowedFields(binder);
	}

	@GetMapping("/registerPage")
	public String registerPage(YadaRegistrationRequest yadaRegistrationRequest) {
		return "/test/security/register";
	}

	/**
	 * Validates the registration form and sends the confirmation email
	 */
	@PostMapping("/signup")
	public String signup(YadaRegistrationRequest yadaRegistrationRequest, BindingResult bindingResult, Model model, HttpServletRequest request, Locale locale, RedirectAttributes redirectAttributes) {
		boolean emailSent = yadaRegistrationController.handleRegistrationRequest(yadaRegistrationRequest, bindingResult, model, request, locale);
		if (!emailSent || bindingResult.hasErrors()) {
			return "/test/security/register";
		}
		yadaNotify.titleKey(redirectAttributes, locale, "yts.register.sent.title").ok().messageKey("yts.register.sent.message").add();
		return OUTCOME_PAGE;
	}

	/**
	 * The link of the registration confirmation email
	 */
	@GetMapping("/registrationConfirmation/{token}")
	public String registrationConfirmation(@PathVariable String token, Locale locale, HttpSession session, RedirectAttributes redirectAttributes) {
		YadaRegistrationOutcome<UserProfile, YadaRegistrationRequest> outcome = yadaRegistrationController.handleRegistrationConfirmation(token, new String[] { REGISTERED_ROLE }, locale, session, UserProfile.class, YadaRegistrationRequest.class);
		switch (outcome.registrationStatus) {
		case OK -> yadaNotify.titleKey(redirectAttributes, locale, "yts.register.confirmed.title").ok().messageKey("yts.register.confirmed.message").add();
		case LINK_EXPIRED -> yadaNotify.titleKey(redirectAttributes, locale, "yts.register.expired.title").error().messageKey("yts.register.expired.message").add();
		case USER_EXISTS -> yadaNotify.titleKey(redirectAttributes, locale, "yts.register.exists.title").error().messageKey("yts.register.exists.message").add();
		case ERROR, REQUEST_INVALID -> yadaNotify.titleKey(redirectAttributes, locale, "yts.register.error.title").error().messageKey("yts.register.error.message").add();
		}
		return OUTCOME_PAGE;
	}

	/**
	 * The password recovery request form, posted to the framework "/yadaPasswordResetPost"
	 * @param email optional value of the email field
	 */
	@GetMapping("/passwordReset")
	public String passwordReset(String email, YadaPasswordResetForm yadaPasswordResetForm) {
		yadaPasswordResetForm.setEmail(email);
		return "/test/security/passwordReset";
	}

	/**
	 * The link of the password recovery email: shows the new password form, posted to the framework "/passwordChangeAfterRequest"
	 */
	@GetMapping("/passwordReset/{token}")
	public String passwordResetForm(@PathVariable String token, Model model, Locale locale, RedirectAttributes redirectAttributes) {
		boolean valid = yadaRegistrationController.passwordResetForm(token, model, redirectAttributes);
		if (!valid) {
			yadaNotify.titleKey(redirectAttributes, locale, "yts.pwdreset.invalidLink.title").error().messageKey("yts.pwdreset.invalidLink.message").add();
			return "redirect:/passwordReset";
		}
		return "/test/security/passwordChange";
	}
}
