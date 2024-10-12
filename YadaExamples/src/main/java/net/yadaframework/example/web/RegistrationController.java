package net.yadaframework.example.web;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.example.core.YexConfiguration;
import net.yadaframework.example.persistence.entity.YexRegistrationRequest;
import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.example.persistence.repository.UserProfileDao;

import net.yadaframework.components.YadaNotify;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.web.YadaRegistrationController;
import net.yadaframework.security.web.YadaRegistrationController.YadaRegistrationOutcome;

/**
 * Application-specific registration methods. All requestMappings must be unprotected.
 */
@Controller
public class RegistrationController {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	// Must be aligned to the config file
	public static final Integer ROLE_USER_ID = 2;
	public static final Integer ROLE_MANAGER_ID = 6;
	public static final Integer ROLE_ADMIN_ID = 8;
	
	private @Autowired HomeController homeController;
	private @Autowired YadaWebUtil yadaWebUtil;
	private @Autowired YadaRegistrationController yadaRegistrationController;
	private @Autowired YexConfiguration config;
	private @Autowired YadaNotify yadaNotify;
	private @Autowired UserProfileDao userProfileDao;
	
	///////////////////////////////////////////////////////////////////////
	////// Registration                                               /////
	///////////////////////////////////////////////////////////////////////

	/**
	 * Open the registration form
	 */
	@RequestMapping("/registerPage")
	public String registerPage(YexRegistrationRequest yexRegistrationRequest) {
		// Return the HTML that contains the registration forms with all the needed fields
		return "/register";
	}
	
	/**
	 * Handle the registration form submission
	 */
	@RequestMapping("/signup")
	public String signup(YexRegistrationRequest yexRegistrationRequest, BindingResult formBinding, Model model, HttpServletRequest request, Locale locale, RedirectAttributes redirectAttributes) {
		// Antispam check: use the field "username" to discover spammers.
		// The idea is that a bot will set a value on a hidden field while humans won't.
		if (StringUtils.isNotEmpty(yexRegistrationRequest.getUsername())) {
			// Just ignore spam submissions and return the homepage with a puzzling "Ok" notification
			yadaNotify.title("Registration", model).ok().message("Ok").add();
			return homeController.home(request, model, locale); // Fix as needed in order to go to the home page
		}
		// Form validation for the fields in the YexRegistrationRequest subclass only. Superclass fields are validated by Yada
		// ValidationUtils.rejectIfEmptyOrWhitespace(formBinding, "name", "validation.empty");
		// ValidationUtils.rejectIfEmptyOrWhitespace(formBinding, "surname", "validation.empty");
		
		// Send the registration confirmation email. Some validation will be performed on the form fields.
		boolean emailSent = yadaRegistrationController.handleRegistrationRequest(yexRegistrationRequest, formBinding, model, request, locale);
		
		if (!emailSent || formBinding.hasErrors()) {
			// Go back to the registration form in case of errors
			return registerPage(yexRegistrationRequest);
		}

		// Redirect to the home page with a confirmation message
		yadaNotify.titleKey(redirectAttributes, locale, "registration.email.confirmed.title").ok().messageKey("registration.email.confirmed.text", yexRegistrationRequest.getEmail()).add();
        return yadaWebUtil.redirectString("/", locale); 
	}	
	
	/**
	 * Handles the confirmation link clicked by the user from the email
	 */
	@RequestMapping("/registrationConfirmation/{token}")
	public String registrationConfirmation(@PathVariable String token, Model model, Locale locale, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpSession session) {
		// Everything is done in the yada class. The outcome must be handled here though.
		// Choose which outcomes to handle and which messages to return.
		YadaRegistrationOutcome<UserProfile, YexRegistrationRequest> outcome = yadaRegistrationController.handleRegistrationConfirmation(token, new String[]{config.getRoleName(ROLE_USER_ID)}, locale, session, UserProfile.class, YexRegistrationRequest.class);
		switch (outcome.registrationStatus) {
		case LINK_EXPIRED:
			yadaNotify.titleKey(redirectAttributes, locale, "registration.confirmation.expired.title").error().messageKey("registration.confirmation.expired.message").add();
			return yadaWebUtil.redirectString("/", locale);
		case USER_EXISTS:
			redirectAttributes.addAttribute("email", outcome.email);
			yadaNotify.titleKey(redirectAttributes, locale, "registration.confirmation.existing.title").error().messageKey("registration.confirmation.existing.message", outcome.email).add();
			return yadaWebUtil.redirectString("/passwordReset", locale);
		case OK:
			// Store the name, surname and all the information asked during registration
			YexRegistrationRequest yexRegistrationRequest = outcome.yadaRegistrationRequest;
			UserProfile userProfile = outcome.userProfile;
			userProfile.setFirstName(yexRegistrationRequest.getName());
			userProfile.setLastName(yexRegistrationRequest.getSurname());
			userProfile = userProfileDao.save(userProfile);
			yadaNotify.titleKey(redirectAttributes, locale, "registration.confirmation.ok.title").ok().messageKey("registration.confirmation.ok.message", outcome.email).add();
			log.info("User '{}' successfully registered", outcome.email);
			return yadaWebUtil.redirectString("/", locale);
		case ERROR:
		case REQUEST_INVALID:
			yadaNotify.titleKey(redirectAttributes, locale, "registration.confirmation.error.title").error().messageKey("registration.confirmation.error.message").add();
			return yadaWebUtil.redirectString("/", locale);
		}
		// Should never get here
		log.error("Invalid registration state - aborting");
		throw new YadaInvalidUsageException("Invalid registration state");
	}
	
	///////////////////////////////////////////////////////////////////////
	////// Password Reset                                             /////
	///////////////////////////////////////////////////////////////////////

	/**
	 * Opens the password reset page
	 * @param email can be sent to prefill the field
	 * @param yadaRegistrationRequest
	 */
	@RequestMapping("/passwordReset")
	public String passwordReset(String email, YadaRegistrationRequest yadaRegistrationRequest) {
		yadaRegistrationRequest.setEmail(email);
		yadaRegistrationRequest.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
		return "/passwordReset";
	}

	/** 
	 * Handler called when clicking on the link in the registration confirmation email
	 * @return
	 */
	@RequestMapping("/passwordReset/{token}")
	public String passwordResetPost(@PathVariable String token, Model model, HttpServletRequest request, RedirectAttributes redirectAttributes, Locale locale) {
		// Everything is done in the yada class.
		boolean done = yadaRegistrationController.passwordResetForm(token, model, redirectAttributes);
		if (!done) {
			yadaNotify.titleKey(redirectAttributes, locale, "pwdreset.invalidlink.title").error().messageKey("pwdreset.invalidlink.message").add();
			return yadaWebUtil.redirectString("/passwordReset", locale); // Moved temporarily
		}
		// Don't do a redirect here because you'll lose the Model
		return homeController.home(request, model, locale); // Fix as needed in order to go to the home page
	}
		
}
