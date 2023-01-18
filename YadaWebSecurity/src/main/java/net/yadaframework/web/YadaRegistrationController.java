package net.yadaframework.web;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.security.YadaUserDetailsService;
import net.yadaframework.security.components.YadaSecurityEmailService;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.components.YadaTokenHandler;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaRegistrationRequestRepository;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsRepository;
import net.yadaframework.security.persistence.repository.YadaUserProfileRepository;
import net.yadaframework.web.form.YadaFormPasswordChange;

@Controller
public class YadaRegistrationController {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaUserCredentialsRepository yadaUserCredentialsRepository;
	@Autowired private YadaUserProfileRepository yadaUserProfileRepository;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	@Autowired private YadaRegistrationRequestRepository yadaRegistrationRequestRepository;
	@Autowired private YadaSecurityEmailService yadaSecurityEmailService;
	@Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private MessageSource messageSource;
	@Autowired private YadaNotify yadaNotify;


	public enum YadaRegistrationStatus {
		OK,					// Good registration
		ERROR,				// Some exception
		REQUEST_INVALID,	// URL format invalid
		LINK_EXPIRED,		// Link expired
		USER_EXISTS;		// User exists
	}

	/**
	 * The outcome of a registration. When successful, the userProfile field should be saved by the caller
	 * @param <T> the subclass of YadaUserProfile
	 */
	public class YadaRegistrationOutcome<T extends YadaUserProfile> {
		/**
		 * The outcome of the registration
		 */
		public YadaRegistrationStatus registrationStatus;
		/**
		 * The new user profile
		 */
		public T userProfile;
		/**
		 * The user email, is null when the registration link is expired
		 */
		public String email;

		public String destinationUrl;

		public YadaRegistrationRequest yadaRegistrationRequest;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////// Registration helpers                                                                         /////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * To be called when the link in the registration confirmation email has been clicked.
	 * It creates a YadaUserCredential instance with basic YadaUserProfile values which should be refined and saved by the caller
	 * @param token
	 * @param userRoles an array of configured roles, like <pre>new String[]{"USER"}</pre>
	 * @param locale
	 * @return a {@link YadaRegistrationOutcome}
	 */
	public <T extends YadaUserProfile> YadaRegistrationOutcome<T> handleRegistrationConfirmation(String token, String[] userRoles, Locale locale, Class<T> userProfileClass) {
		YadaRegistrationOutcome<T> result = new YadaRegistrationOutcome<>();
		long[] parts = yadaTokenHandler.parseLink(token);
		try {
			if (parts!=null) {
				List<YadaRegistrationRequest> registrationRequests = yadaRegistrationRequestRepository.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]);
				if (registrationRequests.isEmpty()) {
					log.warn("registrationRequests.isEmpty()");
					result.registrationStatus = YadaRegistrationStatus.LINK_EXPIRED;
					return result;
				}
				YadaRegistrationRequest registrationRequest = registrationRequests.get(0);
				String email = registrationRequest.getEmail().toLowerCase();
				String destinationUrl = registrationRequest.getDestinationUrl();
				result.email = email;
				result.destinationUrl = destinationUrl;
				result.yadaRegistrationRequest = registrationRequest;
				YadaUserCredentials existing = yadaUserCredentialsRepository.findFirstByUsername(email);
				if (existing!=null) {
					log.warn("Email '{}' already exists", email);
					result.registrationStatus = YadaRegistrationStatus.USER_EXISTS;
					return result;
				}
				// Create new user
				T userProfile = createNewUser(registrationRequest.getEmail(), registrationRequest.getPassword(), userRoles, locale, userProfileClass);
				result.userProfile = userProfile;
				//
				yadaRegistrationRequestRepository.delete(registrationRequest);
				yadaUserDetailsService.authenticateAs(userProfile.getUserCredentials());
				//
				log.info("Registration of '{}' successful", email);
				result.registrationStatus = YadaRegistrationStatus.OK;
				return result;
			} else {
				result.registrationStatus = YadaRegistrationStatus.REQUEST_INVALID;
				log.error("Invalid registration url");
			}
		} catch (Exception e) {
			result.registrationStatus = YadaRegistrationStatus.ERROR;
			log.error("Registration of token {} failed", token, e);
		}
		return result;
	}

	/**
	 * Create a new user. Throws a runtime exception if the email already exists
	 * @param email
	 * @param clearPassword cleartext password
	 * @param userRoles
	 * @param locale
	 * @param userProfileClass
	 * @return
	 */
	public <T extends YadaUserProfile> T createNewUser(String email, String clearPassword, String[] userRoles, Locale locale, Class<T> userProfileClass) {
		try {
			YadaUserCredentials userCredentials = new YadaUserCredentials();
			userCredentials.setUsername(email.toLowerCase());
			userCredentials.changePassword(clearPassword, passwordEncoder);
			userCredentials.setEnabled(true);
			userCredentials.addRoles(yadaConfiguration.getRoleIds(userRoles));
			T userProfile = userProfileClass.newInstance();
			userProfile.setLocale(locale);
			userProfile.setUserCredentials(userCredentials);
			return (T) yadaUserProfileRepository.save(userProfile);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new YadaInternalException("Can't create instance of {}", userProfileClass);
		}
	}

	/**
	 * This method should be called by a registration controller to perform the actual registration
	 * @param yadaRegistrationRequest
	 * @param bindingResult
	 * @param model the flag "yadaUserExists" il set to true when the user already exists
	 * @param locale
	 * @return true if the user has been registered, false otherwise
	 */
	public boolean handleRegistrationRequest(YadaRegistrationRequest yadaRegistrationRequest, BindingResult bindingResult, Model model, HttpServletRequest request, Locale locale) {
		//
		// Validation
		//
		String email = yadaRegistrationRequest.getEmail();
		// Simple email validation, email blacklisting
		if (StringUtils.isBlank(email) || email.indexOf("@")<0 || email.indexOf(".")<0 || yadaConfiguration.emailBlacklisted(email)) {
			bindingResult.rejectValue("email", "yada.form.registration.email.invalid");
		} else {
			email = email.toLowerCase(locale);
			yadaRegistrationRequest.setEmail(email);
		}
		// Check if user exists
		YadaUserCredentials existing = yadaUserCredentialsRepository.findFirstByUsername(email);
		if (existing!=null) {
			log.debug("Email {} already found in database while registering new user", email);
			bindingResult.rejectValue("email", "yada.form.registration.username.exists");
			model.addAttribute("yadaUserExists", true);
		}
		if (!yadaUserDetailsService.validatePasswordSyntax(yadaRegistrationRequest.getPassword())) {
			bindingResult.rejectValue("password", "yada.form.registration.password.length", new Object[]{yadaConfiguration.getMinPasswordLength(), yadaConfiguration.getMaxPasswordLength()}, "Wrong password length");
		}
		if (bindingResult.hasErrors()) {
			return false;
		}

		//
		// Add registration request to database
		//
		yadaRegistrationRequest.setRegistrationType(YadaRegistrationType.REGISTRATION);
		// Cleanup of old requests
		yadaSecurityUtil.registrationRequestCleanup(yadaRegistrationRequest);
		yadaRegistrationRequest = yadaRegistrationRequestRepository.save(yadaRegistrationRequest); // To get id and token
		boolean emailSent = yadaSecurityEmailService.sendRegistrationConfirmation(yadaRegistrationRequest, null, request, locale);
		if (!emailSent) {
			yadaRegistrationRequestRepository.delete(yadaRegistrationRequest);
			log.debug("Registration mail not sent to {}", email);
			bindingResult.rejectValue("email", "yada.form.registration.email.failed");
			return false;
		}
		return true;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////// Password Recovery                                                                            /////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/** Default method to change a user password.
	 *
	 * @return
	 */
	@RequestMapping("/passwordChangeAfterRequest")
	public String passwordChangeAfterRequest(YadaFormPasswordChange yadaFormPasswordChange, BindingResult bindingResult, Model model, Locale locale) {
		if (!yadaUserDetailsService.validatePasswordSyntax(yadaFormPasswordChange.getPassword())) {
			bindingResult.rejectValue("password", "validation.password.length", new Object[]{yadaConfiguration.getMinPasswordLength(), yadaConfiguration.getMaxPasswordLength()}, "Wrong password length");
			return "/yada/modalPasswordChange";
		}
		boolean done = yadaSecurityUtil.performPasswordChange(yadaFormPasswordChange);
		if (done) {
			log.info("Password changed for user '{}'", yadaFormPasswordChange.getUsername());
			yadaNotify.title("Password Changed", model).ok().message("Your password has been changed").add();
			model.addAttribute("pwdChangeOk", "pwdChangeOk");
		} else {
			log.error("Password change failed for user '{}'", yadaFormPasswordChange.getUsername());
			model.addAttribute("fatalError", "fatalError");
			yadaNotify.title("Password change failed", model).ok().message("An error occurred while changing the password. Please try again and contact us if the problem persists").add();
		}
		return "/yada/modalPasswordChange";
	}

	/**
	 * Called via ajax to open the final password change modal
	 * @param token
	 * @param username
	 * @return
	 */
	@RequestMapping("/yadaPasswordChangeModal/{token}/{username}/end")
	public String passwordChangeModal(@PathVariable("token") String token, @PathVariable("username") String username, @ModelAttribute YadaFormPasswordChange yadaFormPasswordChange) {
		return "/yada/modalPasswordChange";
	}

	/** To be called in the controller that handles the password recovery link in the email.
	 * It creates these model attributes: username, token, dialogType=passwordRecovery
	 * @return false for an invalid request, true if valid
	 */
	public boolean passwordResetForm(String token, Model model, RedirectAttributes redirectAttributes) {
		long[] parts = yadaTokenHandler.parseLink(token);
		try {
			if (parts!=null) {
				List<YadaRegistrationRequest> registrationRequests = yadaRegistrationRequestRepository.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]);
				if (registrationRequests.isEmpty()) {
					// TODO remove this message, the caller should add its own
					yadaWebUtil.modalError("Password change failed", "The link for password change is expired. Please repeat the change request from the start.", redirectAttributes);
					return false;
				}
				YadaRegistrationRequest registrationRequest = registrationRequests.get(0);
				model.addAttribute("username", registrationRequest.getEmail());
				model.addAttribute("token", token);
				model.addAttribute("dialogType", "passwordRecovery");
				return true;
			}
		} catch (Exception e) {
			log.debug("Recupero Password Fallito", e);
		}
		// TODO remove this message, the caller should add its own
		yadaWebUtil.modalError("Password change failed", "An error occurred while changing the password. Please try again and contact us if the problem persists.", redirectAttributes);
		return false;
	}

	@RequestMapping("/yadaPasswordResetPost")
	public String passwordRecovery(YadaRegistrationRequest yadaRegistrationRequest, BindingResult bindingResult, Locale locale, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
		if (yadaRegistrationRequest==null || yadaRegistrationRequest.getEmail()==null) {
			return "redirect:/passwordReset";
		}
		// Controllo se esiste un utente
		String email = yadaRegistrationRequest.getEmail();
		List<YadaUserCredentials> existing = yadaUserCredentialsRepository.findByUsername(StringUtils.trimToEmpty(email).toLowerCase(locale), yadaWebUtil.FIND_ONE);
		if (existing.isEmpty()) {
			bindingResult.rejectValue("email", "yada.passwordrecover.username.notfound");
			return "/passwordReset";
		}
		yadaRegistrationRequest.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
		// Pulisco le vecchie richieste
		yadaSecurityUtil.registrationRequestCleanup(yadaRegistrationRequest);
		yadaRegistrationRequest.setPassword("fakefake"); // La metto solo per evitare un errore di validazione al save
		yadaRegistrationRequest = yadaRegistrationRequestRepository.save(yadaRegistrationRequest); // Va fatto subito per avere l'id e il token
		boolean emailSent = yadaSecurityEmailService.sendPasswordRecovery(yadaRegistrationRequest, request, locale);
		if (!emailSent) {
			yadaRegistrationRequestRepository.delete(yadaRegistrationRequest);
			log.debug("Sending email to {} failed while resetting password", yadaRegistrationRequest.getEmail());
			bindingResult.rejectValue("email", "yada.email.send.failed");
			return "/passwordReset";
		}
		yadaNotify.titleKey(redirectAttributes, locale, "yada.email.passwordrecover.title")
			.ok()
			.messageKey("yada.email.passwordrecover.message", yadaRegistrationRequest.getEmail())
			.add();
		String address = yadaConfiguration.getPasswordResetSent(locale);

        return "redirect:" + address;
	}
}
