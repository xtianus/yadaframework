package net.yadaframework.security.web;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.components.YadaNotify;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.security.components.YadaSecurityEmailService;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.components.YadaTokenHandler;
import net.yadaframework.security.components.YadaUserDetailsService;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaRegistrationRequestDao;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.persistence.repository.YadaUserProfileDao;
import net.yadaframework.web.YadaViews;
import net.yadaframework.web.form.YadaFormPasswordChange;

@Controller
public class YadaRegistrationController {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaUserProfileDao yadaUserProfileDao;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	@Autowired private YadaRegistrationRequestDao yadaRegistrationRequestDao;
	@Autowired private YadaSecurityEmailService yadaSecurityEmailService;
	@Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaNotify yadaNotify;

	// For some reason, autowiring of the "passwordEncoder" created by YadaSecurityConfig doesn't work: bean is not found
	/*@Autowired */ private PasswordEncoder passwordEncoder = null;

	public enum YadaRegistrationStatus {
		OK,					// Good registration
		ERROR,				// Some exception
		REQUEST_INVALID,	// URL format invalid
		LINK_EXPIRED,		// Link expired
		USER_EXISTS;		// User exists
	}
	
	public enum YadaChangeUsernameResult {
		OK,					// Good
		ERROR,				// Some exception
		REQUEST_INVALID,	// URL format invalid
		LINK_EXPIRED,		// Link expired
		USER_EXISTS;		// User exists
	}

	/**
	 * The outcome of a registration. When successful, the userProfile field should be saved by the caller
	 * @param <T> the subclass of YadaUserProfile
	 */
	public class YadaRegistrationOutcome<T extends YadaUserProfile, R extends YadaRegistrationRequest> {
		public YadaRegistrationStatus registrationStatus; // The outcome of the registration
		public T userProfile; // The new user profile
		public String email; // The user email, is null when the registration link is expired
		// Deprecated for security reasons
		// public String destinationUrl;
		public R yadaRegistrationRequest; // This can be a subclass with extra data
	}
	
	public class YadaChangeUsernameOutcome {
		public YadaChangeUsernameResult resultCode;
		public String newUsername;
		YadaChangeUsernameOutcome setCode(YadaChangeUsernameResult code) {
			this.resultCode = code;
			return this;
		}
	}
		
	@PostConstruct
	public void init() {
		if (yadaConfiguration.encodePassword()) {
			passwordEncoder = new BCryptPasswordEncoder();
		}

	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////// Registration helpers                                                                         /////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * To be called when the link in the registration confirmation email has been clicked.
	 * It creates a YadaUserCredential instance with basic YadaUserProfile values which should be refined and saved by the caller
	 * @param token
	 * @param userRoles an array of configured roles, like <pre>new String[]{"USER"}</pre>. 
	 * The role name must be exactly as configured in &lt;security>&lt;roles>&lt;role>&lt;key>
	 * @param locale
	 * @return a {@link YadaRegistrationOutcome}
	 */
	public <T extends YadaUserProfile, R extends YadaRegistrationRequest> YadaRegistrationOutcome<T, R> handleRegistrationConfirmation(String token, String[] userRoles, Locale locale, HttpSession session, Class<T> userProfileClass, Class<R> registrationRequestClass) {
		// We invalidate the current session in case the registering user is already logged in for some reason
		try {
			session.invalidate();
		} catch (Exception e) {
			log.debug("Error invalidating session at user registration (ignored)", e);
		}

		YadaRegistrationOutcome<T, R> result = new YadaRegistrationOutcome<>();
		long[] parts = yadaTokenHandler.parseLink(token);
		try {
			if (parts!=null) {
				List<R> registrationRequests = yadaRegistrationRequestDao.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1], registrationRequestClass);
				if (registrationRequests.isEmpty()) {
					log.warn("registrationRequests.isEmpty()");
					result.registrationStatus = YadaRegistrationStatus.LINK_EXPIRED;
					return result;
				}
				R registrationRequest = registrationRequests.get(0);
				String email = registrationRequest.getEmail().toLowerCase(Locale.ROOT);
				// String destinationUrl = registrationRequest.getDestinationUrl(); // This was a security issue
				result.email = email;
				// result.destinationUrl = destinationUrl;
				result.yadaRegistrationRequest = registrationRequest; // This can be a subclass with extra data
				YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(email);
				if (existing!=null) {
					log.warn("Email '{}' already exists", email);
					result.registrationStatus = YadaRegistrationStatus.USER_EXISTS;
					return result;
				}
				// Create new user
				T userProfile = createNewUser(registrationRequest.getEmail(), registrationRequest.getPassword(), userRoles, locale, userProfileClass);
				result.userProfile = userProfile;
				//
				yadaRegistrationRequestDao.delete(registrationRequest);
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
			return (T) yadaUserProfileDao.save(userProfile);
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
			email = email.toLowerCase(Locale.ROOT);
			yadaRegistrationRequest.setEmail(email);
		}
		// Check if user exists
		YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(email);
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
		yadaRegistrationRequest = yadaRegistrationRequestDao.save(yadaRegistrationRequest); // To get id and token
		boolean emailSent = yadaSecurityEmailService.sendRegistrationConfirmation(yadaRegistrationRequest, null, request, locale);
		if (!emailSent) {
			yadaRegistrationRequestDao.delete(yadaRegistrationRequest);
			log.debug("Registration mail not sent to {}", email);
			bindingResult.rejectValue("email", "yada.form.registration.email.failed");
			return false;
		}
		return true;
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////// Password Recovery                                                                            /////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/** 
	 * Default method to change a user password.
	 * @return
	 */
	@RequestMapping("/passwordChangeAfterRequest") // Ajax
	public String passwordChangeAfterRequest(YadaFormPasswordChange yadaFormPasswordChange, BindingResult bindingResult, Model model, Locale locale) {
		// TODO view-related code should be moved to the application controller
		if (!yadaUserDetailsService.validatePasswordSyntax(yadaFormPasswordChange.getPassword())) {
			bindingResult.rejectValue("password", "validation.password.length", new Object[]{yadaConfiguration.getMinPasswordLength(), yadaConfiguration.getMaxPasswordLength()}, "Wrong password length");
			return "/yada/modalPasswordChange";
		}
		boolean done = yadaSecurityUtil.performPasswordChange(yadaFormPasswordChange);
		if (done) {
			log.info("Password changed for user '{}'", yadaFormPasswordChange.getUsername());
			// redirectOnClose() is to show the logged in version of the site (no "login" link)
			yadaNotify.titleKey(model, locale, "yada.pwdreset.done.title").ok().messageKey("yada.pwdreset.done.message").redirectOnClose("/").add();
			model.addAttribute("pwdChangeOk", "pwdChangeOk");
		} else {
			log.error("Password change failed for user '{}'", yadaFormPasswordChange.getUsername());
			model.addAttribute("fatalError", "fatalError");
			yadaNotify.titleKey(model, locale, "yada.pwdreset.failed.title").error().messageKey("yada.pwdreset.failed.message").add();
		}
		return YadaViews.AJAX_NOTIFY;
	}

	/**
	 * Called via ajax to open the final password change modal
	 * @param token
	 * @param username
	 * @return
	 */
	@Deprecated // This is application-specific and should be moved to the application
	@RequestMapping("/yadaPasswordChangeModal/{token}/{username}/end")
	public String passwordChangeModal(@PathVariable String token, @PathVariable String username, @ModelAttribute YadaFormPasswordChange yadaFormPasswordChange) {
		return "/yada/modalPasswordChange";
	}

	/** To be called in the controller that handles the password recovery link in the email.
	 * It creates these model attributes: username, token, dialogType=passwordRecovery
	 * @return false for an invalid request (link expired), true if valid
	 */
	public boolean passwordResetForm(String token, Model model, RedirectAttributes redirectAttributes) {
		long[] parts = yadaTokenHandler.parseLink(token);
		if (parts!=null) {
			List<YadaRegistrationRequest> registrationRequests = yadaRegistrationRequestDao.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1], YadaRegistrationRequest.class);
			if (registrationRequests.isEmpty()) {
				return false;
			}
			YadaRegistrationRequest registrationRequest = registrationRequests.get(0);
			model.addAttribute("username", registrationRequest.getEmail());
			model.addAttribute("token", token);
			model.addAttribute("dialogType", "passwordRecovery");
			return true;
		}
		return false;
	}

	/**
	 * Handles the password reset form
	 * @param yadaRegistrationRequest will contain the email address that requires a password reset
	 * @param bindingResult
	 * @param locale
	 * @param redirectAttributes
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/yadaPasswordResetPost")
	public String yadaPasswordResetPost(YadaRegistrationRequest yadaRegistrationRequest, BindingResult bindingResult, Locale locale, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
		// TODO view-related code should be moved to the application controller
		if (yadaRegistrationRequest==null || yadaRegistrationRequest.getEmail()==null) {
			return "redirect:/passwordReset";
		}
		// Check if a user actually exists
		String email = yadaRegistrationRequest.getEmail();
		YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(StringUtils.trimToEmpty(email).toLowerCase(Locale.ROOT));
		if (existing==null) {
			bindingResult.rejectValue("email", "yada.passwordrecover.username.notfound");
			return "/passwordReset";
		}
		yadaRegistrationRequest.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
		// Pulisco le vecchie richieste
		yadaSecurityUtil.registrationRequestCleanup(yadaRegistrationRequest);
		// yadaRegistrationRequest.setPassword("fakefake"); // La metto solo per evitare un errore di validazione al save
		yadaRegistrationRequest = yadaRegistrationRequestDao.save(yadaRegistrationRequest); // Save here to get id and token
		boolean emailSent = yadaSecurityEmailService.sendPasswordRecovery(yadaRegistrationRequest, request, locale);
		if (!emailSent) {
			yadaRegistrationRequestDao.delete(yadaRegistrationRequest);
			log.debug("Sending email to {} failed while resetting password", yadaRegistrationRequest.getEmail());
			bindingResult.rejectValue("email", "yada.email.send.failed");
			return "/passwordReset";
		}
		yadaNotify.titleKey(redirectAttributes, locale, "yada.email.passwordrecover.title")
			.ok()
			.messageKey("yada.email.passwordrecover.message")
			// .messageKey("yada.email.passwordrecover.message", yadaRegistrationRequest.getEmail())
			.add();
		String address = yadaConfiguration.getPasswordResetSent(locale);

        return "redirect:" + address;
	}
	
	/**
	 * Change a username after the user clicked on the confirmation email link
	 * @param fromUsername the old username (email)
	 * @param token
	 * @param locale
	 * @return
	 */
	public YadaChangeUsernameOutcome changeUsername(String token) {
		YadaChangeUsernameOutcome result = new YadaChangeUsernameOutcome();
		long[] parts = yadaTokenHandler.parseLink(token);
		try {
			if (parts != null) {
				List<YadaRegistrationRequest> registrationRequests = yadaRegistrationRequestDao
						.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1], YadaRegistrationRequest.class);
				if (registrationRequests.isEmpty()) {
					return result.setCode(YadaChangeUsernameResult.LINK_EXPIRED);
				}
				YadaRegistrationRequest registrationRequest = registrationRequests.get(0);
				String newUsername = registrationRequest.getEmail();
				result.newUsername = newUsername;
				// Check that in the meantime no other user with that email has been registered
				YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(newUsername.toLowerCase(Locale.ROOT));
				if (existing!=null) {
					yadaRegistrationRequestDao.delete(registrationRequest);
					return result.setCode(YadaChangeUsernameResult.USER_EXISTS);
				}
				String previousUsername = registrationRequest.getYadaUserCredentials().getUsername();
				yadaRegistrationRequestDao.delete(registrationRequest);
				boolean changed = yadaUserCredentialsDao.changeUsername(previousUsername, newUsername);
				if (!changed) {
					return result.setCode(YadaChangeUsernameResult.ERROR);
				}
				return result.setCode(YadaChangeUsernameResult.OK);
			} else {
				return result.setCode(YadaChangeUsernameResult.REQUEST_INVALID);
			}
		} catch (Exception e) {
			log.debug("Can't change username", e);
			return result.setCode(YadaChangeUsernameResult.ERROR);
		}
	}	
}
