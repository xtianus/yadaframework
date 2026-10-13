package net.yadaframework.security.web;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.components.YadaNotify;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaRegistrationType;
import net.yadaframework.exceptions.YadaInternalException;
import net.yadaframework.exceptions.YadaInvalidUsageException;
import net.yadaframework.security.components.YadaSecurityEmailService;
import net.yadaframework.security.components.YadaSecurityTokenCodec.ParsedSecurityToken;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.components.YadaTokenHandler;
import net.yadaframework.security.components.YadaUserDetailsService;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaRegistrationRequestDao;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.persistence.repository.YadaUserProfileDao;
import net.yadaframework.web.form.YadaFormPasswordChange;

/**
 * Registration, password recovery and email change. Every flow is confirmed by a single-use link with an expiring bearer
 * token (see YadaRegistrationRequestDao): links are checked for their own purpose and consumed in the same transaction
 * that performs the change.
 * <p>
 * Registration requests must never be bound from a form as a whole entity: the application controller restricts the
 * binding with {@link #setRegistrationAllowedFields(WebDataBinder, String...)} in an {@code @InitBinder} method, and
 * {@link #handleRegistrationRequest(YadaRegistrationRequest, BindingResult, Model, HttpServletRequest, Locale)} rejects
 * requests that already have an identity.
 */
@Controller
public class YadaRegistrationController {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	/** The fields of YadaRegistrationRequest that a registration form may set */
	public static final String[] REGISTRATION_FORM_FIELDS = { "email", "password", "confirmPassword" };

	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaUserProfileDao yadaUserProfileDao;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	@Autowired private YadaRegistrationRequestDao yadaRegistrationRequestDao;
	@Autowired private YadaSecurityEmailService yadaSecurityEmailService;
	@Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private YadaNotify yadaNotify;
	@Autowired private YadaWebUtil yadaWebUtil;

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

	/**
	 * Restricts the binding of a registration form to the {@link #REGISTRATION_FORM_FIELDS} and the given fields of a
	 * subclass, so that the id, the token, the type, the user or the timestamps of a request can't be set from the
	 * request parameters. Call it from an {@code @InitBinder} method of the application controller, e.g.
	 * <pre>
	 * &#64;InitBinder("yexRegistrationRequest")
	 * void registrationBinder(WebDataBinder binder) {
	 * 	YadaRegistrationController.setRegistrationAllowedFields(binder, "name", "surname");
	 * }
	 * </pre>
	 * Parameters of other fields are ignored.
	 * @param binder the binder of the registration request model attribute
	 * @param subclassFields the extra fields that the form may set, e.g. "name"
	 */
	public static void setRegistrationAllowedFields(WebDataBinder binder, String... subclassFields) {
		List<String> allowed = new ArrayList<>(Arrays.asList(REGISTRATION_FORM_FIELDS));
		allowed.addAll(Arrays.asList(subclassFields));
		binder.setAllowedFields(allowed.toArray(new String[0]));
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////// Registration helpers                                                                         /////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * To be called when the link in the registration confirmation email has been clicked.
	 * It creates a YadaUserCredential instance with basic YadaUserProfile values which should be refined and saved by the caller.
	 * The link must be a valid, unexpired registration link; it is consumed in the transaction that creates the user, so
	 * it works once.
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
		ParsedSecurityToken parsedToken = yadaTokenHandler.parseLink(token);
		if (parsedToken==null) {
			result.registrationStatus = YadaRegistrationStatus.REQUEST_INVALID;
			log.warn("Invalid registration url");
			return result;
		}
		try {
			Optional<YadaRegistrationOutcome<T, R>> consumed = yadaRegistrationRequestDao.consume(parsedToken, YadaRegistrationType.REGISTRATION, registrationRequestClass, registrationRequest -> {
				String email = registrationRequest.getEmail().toLowerCase(Locale.ROOT);
				// String destinationUrl = registrationRequest.getDestinationUrl(); // This was a security issue
				result.email = email;
				result.yadaRegistrationRequest = registrationRequest; // This can be a subclass with extra data
				YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(email);
				if (existing!=null) {
					log.warn("Email '{}' already exists", email);
					result.registrationStatus = YadaRegistrationStatus.USER_EXISTS;
					return result;
				}
				// Create new user
				result.userProfile = createNewUser(registrationRequest.getEmail(), registrationRequest.getPassword(), userRoles, locale, userProfileClass);
				result.registrationStatus = YadaRegistrationStatus.OK;
				return result;
			});
			if (consumed.isEmpty()) {
				log.warn("Registration request {} not found, expired or of another type", parsedToken.id());
				result.registrationStatus = YadaRegistrationStatus.LINK_EXPIRED;
				return result;
			}
			if (result.registrationStatus==YadaRegistrationStatus.OK) {
				// Authentication after the commit
				yadaUserDetailsService.authenticateAs(result.userProfile.getUserCredentials());
				log.info("Registration of '{}' successful", result.email);
			}
			return result;
		} catch (Exception e) {
			result.registrationStatus = YadaRegistrationStatus.ERROR;
			log.error("Registration with request {} failed", parsedToken.id(), e);
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
		// A bound id or user would let the form overwrite an existing request or attach it to an account
		if (yadaRegistrationRequest.getId()!=null || yadaRegistrationRequest.getYadaUserCredentials()!=null) {
			throw new YadaInvalidUsageException("A registration request from a form must be new: restrict the binding with setRegistrationAllowedFields()");
		}
		//
		// Validation
		//
		String email = yadaRegistrationRequest.getEmail();
		// Simple email validation, email blacklisting
		if (StringUtils.isBlank(email) || email.indexOf("@")<0 || email.indexOf(".")<0 || yadaConfiguration.emailBlacklisted(email)) {
			bindingResult.rejectValue("email", "yada.form.registration.email.invalid");
		} else {
			// Even though there might exist case-sensitive email addresses, handling them is too painful
			// so we just force people into using case-insensitive addresses for the sake of everyone.
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
		yadaRegistrationRequest = yadaRegistrationRequestDao.issue(yadaRegistrationRequest); // To get id and token
		boolean emailSent = yadaSecurityEmailService.sendRegistrationConfirmation(yadaRegistrationRequest, null, request, locale);
		if (!emailSent) {
			log.debug("Registration mail not sent to {} (request {})", email, yadaRegistrationRequest.getId());
			if (!yadaConfiguration.isDevelopmentEnvironment()) {
				yadaRegistrationRequestDao.delete(yadaRegistrationRequest);
			} else {
				log.debug("Registration request still valid in development: copy&paste the registration url");
			}
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
		return yadaNotify.getViewName();
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
	 * It creates these model attributes: username, token, dialogType=passwordRecovery.
	 * The link is checked like the final password change does (purpose and expiration) but is not used.
	 * @return false for an invalid request (link expired), true if valid
	 */
	public boolean passwordResetForm(String token, Model model, RedirectAttributes redirectAttributes) {
		ParsedSecurityToken parsedToken = yadaTokenHandler.parseLink(token);
		YadaRegistrationRequest registrationRequest = yadaRegistrationRequestDao.findValid(parsedToken, YadaRegistrationType.PASSWORD_RECOVERY, YadaRegistrationRequest.class);
		if (registrationRequest==null) {
			return false;
		}
		model.addAttribute("username", registrationRequest.getEmail());
		model.addAttribute("token", token);
		model.addAttribute("dialogType", "passwordRecovery");
		return true;
	}

	/**
	 * Handles the password reset form: when a user with the submitted email exists, a new password recovery request
	 * replaces the previous ones and its link is emailed. Only the email is read from the request.
	 * @param yadaPasswordResetForm will contain the email address that requires a password reset
	 * @param bindingResult
	 * @param locale
	 * @param redirectAttributes
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping("/yadaPasswordResetPost")
	public String yadaPasswordResetPost(YadaPasswordResetForm yadaPasswordResetForm, BindingResult bindingResult, Locale locale, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
		boolean emailSent = false;
		String targetView = yadaConfiguration.getPasswordResetSent(locale);
		// TODO view-related code should be moved to the application controller
		String email = yadaPasswordResetForm==null ? null : StringUtils.trimToNull(yadaPasswordResetForm.getEmail());
		if (email==null) {
			return "redirect:/passwordReset";
		}
		email = email.toLowerCase(Locale.ROOT);
		// Check if a user actually exists
		YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(email);
		if (existing!=null) {
			YadaRegistrationRequest recoveryRequest = new YadaRegistrationRequest();
			recoveryRequest.setEmail(email);
			recoveryRequest.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
			// Pulisco le vecchie richieste
			yadaSecurityUtil.registrationRequestCleanup(recoveryRequest);
			recoveryRequest.setPassword("fakefake"); // La metto solo per evitare un errore di validazione al save
			recoveryRequest = yadaRegistrationRequestDao.issue(recoveryRequest); // Save here to get id and token
			emailSent = yadaSecurityEmailService.sendPasswordRecovery(recoveryRequest, request, locale);
			if (!emailSent) {
				yadaRegistrationRequestDao.delete(recoveryRequest);
				log.debug("Sending email to {} failed while resetting password", recoveryRequest.getEmail());
				yadaNotify.titleKey(redirectAttributes, locale, "yada.email.passwordrecover.title")
				.ok().messageKey("yada.email.send.failed").add();				
			}
		}
		if (emailSent || existing==null) {
			yadaNotify.titleKey(redirectAttributes, locale, "yada.email.passwordrecover.title")
			.ok().messageKey("yada.email.passwordrecover.message")
			.add();
		}

        return yadaWebUtil.redirectString(targetView, locale);
	}

	/**
	 * Sends the confirmation email of an email (username) change. The new address is used when the user opens the
	 * link, see {@link #changeUsername(String)}. A new request replaces the previous ones of the same user.
	 * @param yadaUserCredentials the user that changes the email
	 * @param newEmail the new email
	 * @param request
	 * @param locale
	 * @return true when the email has been sent
	 */
	public boolean handleEmailChangeRequest(YadaUserCredentials yadaUserCredentials, String newEmail, HttpServletRequest request, Locale locale) {
		YadaRegistrationRequest changeRequest = new YadaRegistrationRequest();
		changeRequest.setEmail(StringUtils.trimToEmpty(newEmail).toLowerCase(Locale.ROOT));
		changeRequest.setRegistrationType(YadaRegistrationType.EMAIL_CHANGE);
		changeRequest.setYadaUserCredentials(yadaUserCredentials);
		changeRequest.setPassword("unused"); // Not null
		for (YadaRegistrationRequest previous : yadaRegistrationRequestDao.findByUserAndRegistrationType(yadaUserCredentials, YadaRegistrationType.EMAIL_CHANGE)) {
			yadaRegistrationRequestDao.delete(previous);
		}
		changeRequest = yadaRegistrationRequestDao.issue(changeRequest);
		boolean emailSent = yadaSecurityEmailService.sendEmailChangeConfirmation(changeRequest, request, locale);
		if (!emailSent) {
			yadaRegistrationRequestDao.delete(changeRequest);
		}
		return emailSent;
	}
	
	/**
	 * Change a username after the user clicked on the confirmation email link.
	 * The link must be a valid, unexpired email change link; it is consumed in the transaction that changes the username.
	 * @param token
	 * @return
	 */
	public YadaChangeUsernameOutcome changeUsername(String token) {
		YadaChangeUsernameOutcome result = new YadaChangeUsernameOutcome();
		ParsedSecurityToken parsedToken = yadaTokenHandler.parseLink(token);
		if (parsedToken==null) {
			return result.setCode(YadaChangeUsernameResult.REQUEST_INVALID);
		}
		try {
			Optional<YadaChangeUsernameOutcome> consumed = yadaRegistrationRequestDao.consume(parsedToken, YadaRegistrationType.EMAIL_CHANGE, YadaRegistrationRequest.class, registrationRequest -> {
				String newUsername = registrationRequest.getEmail();
				result.newUsername = newUsername;
				// Check that in the meantime no other user with that email has been registered
				YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(newUsername.toLowerCase(Locale.ROOT));
				if (existing!=null) {
					return result.setCode(YadaChangeUsernameResult.USER_EXISTS);
				}
				if (registrationRequest.getYadaUserCredentials()==null) {
					return result.setCode(YadaChangeUsernameResult.ERROR);
				}
				String previousUsername = registrationRequest.getYadaUserCredentials().getUsername();
				boolean changed = yadaUserCredentialsDao.changeUsername(previousUsername, newUsername);
				return result.setCode(changed ? YadaChangeUsernameResult.OK : YadaChangeUsernameResult.ERROR);
			});
			if (consumed.isEmpty()) {
				return result.setCode(YadaChangeUsernameResult.LINK_EXPIRED);
			}
			return result;
		} catch (Exception e) {
			log.debug("Can't change username", e);
			return result.setCode(YadaChangeUsernameResult.ERROR);
		}
	}	

}
