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
import net.yadaframework.security.YadaUserDetailsService;
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
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////// Registration helpers                                                                         /////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * To be called when the link in the registration confirmation email has been clicked. 
	 * It creates a YadaUserCredential instance with basic YadaUserProfile values which should be refined and saved by the caller 
	 * @param token
	 * @param userRoles
	 * @param locale
	 * @return a {@link YadaRegistrationOutcome}
	 */
	public <T extends YadaUserProfile> YadaRegistrationOutcome<T> handleRegistrationConfirmation(String token, String[] userRoles, Locale locale, Class<T> userProfileClass) {
		YadaRegistrationOutcome<T> result = new YadaRegistrationOutcome<T>();
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
				String email = registrationRequest.getEmail();
				result.email = email;
				YadaUserCredentials existing = yadaUserCredentialsRepository.findFirstByUsername(email);
				if (existing!=null) {
					log.warn("Email '{}' already exists", email);
					result.registrationStatus = YadaRegistrationStatus.USER_EXISTS;
					return result;
				}
				// Create new user
				YadaUserCredentials userCredentials = new YadaUserCredentials();
				userCredentials.setUsername(registrationRequest.getEmail());
				userCredentials.changePassword(registrationRequest.getPassword(), passwordEncoder);
				userCredentials.setEnabled(true);
				userCredentials.addRoles(yadaConfiguration.getRoleIds(userRoles));
				T userProfile = userProfileClass.newInstance();
				userProfile.setLocale(locale);
				userProfile.setUserCredentials(userCredentials);
				result.userProfile = userProfile;
				//
				yadaRegistrationRequestRepository.delete(registrationRequest);
				yadaUserDetailsService.authenticateAs(userCredentials);
				log.info("Registration: user='{}' password='{}'", registrationRequest.getEmail(), registrationRequest.getPassword());
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

	/** Metodo chiamato al submit del form di cambio password.
	 * 
	 * @return
	 */
	@RequestMapping("/passwordChangeAfterRequest")
	public String passwordChangeAfterRequest(YadaFormPasswordChange yadaFormPasswordChange, BindingResult bindingResult, Model model, Locale locale) {
		if (!yadaUserDetailsService.validatePasswordSyntax(yadaFormPasswordChange.getPassword())) {
			bindingResult.rejectValue("password", "validation.password.length", new Object[]{yadaConfiguration.getMinPasswordLength(), yadaConfiguration.getMaxPasswordLength()}, "Wrong password length");
			return "/yada/modalPasswordChange";
		}
		long[] parts = yadaTokenHandler.parseLink(yadaFormPasswordChange.getToken());
		boolean fatalError=true;
		try {
			if (parts!=null) {
				YadaRegistrationRequest registrationRequest = yadaRegistrationRequestRepository.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]).get(0);
				YadaUserCredentials yadaUserCredentials = null;
				String username = registrationRequest.getEmail();
				List<YadaUserCredentials> credentials = yadaUserCredentialsRepository.findByUsername(StringUtils.trimToEmpty(username).toLowerCase(locale), yadaWebUtil.FIND_ONE);
				if (credentials.size()==1) {
					yadaUserCredentials = credentials.get(0);
					yadaUserCredentials.changePassword(yadaFormPasswordChange.getPassword(), passwordEncoder);
					yadaUserCredentialsRepository.save(yadaUserCredentials);
					yadaRegistrationRequestRepository.delete(registrationRequest);
					fatalError=false;
					if (yadaUserCredentials.isEnabled()) {
						yadaUserDetailsService.authenticateAs(yadaUserCredentials);
					}
					log.info("PASSWORD CHANGE: user='{}' password='{}'", username, yadaFormPasswordChange.getPassword());
					yadaWebUtil.modalOk("Password Changed", "Your password has been changed.", model);
					model.addAttribute("pwdChangeOk", "pwdChangeOk");
				}
			}
		} catch (Exception e) {
			fatalError=true;
			log.info("Password change failed", e);
		}
		if (fatalError) {
			log.error("Password change failed (fatalError)");
			model.addAttribute("fatalError", "fatalError");
			yadaWebUtil.modalError("Password change failed", "An error occurred while changing the password. Please try again and contact us if the problem persists.", model);
		}
		return "/yada/modalPasswordChange";
		
	}
	
	/**
	 * Chiamato dalla home per aprire il modal di cambio password la prima volta
	 * @param token
	 * @param username
	 * @return
	 */
	@RequestMapping("/passwordChangeModal/{token}/{username}/end")
	public String passwordChangeModal(@PathVariable String token, @PathVariable String username, @ModelAttribute YadaFormPasswordChange yadaFormPasswordChange) {
		return "/yada/modalPasswordChange";
	}

	/** Metodo chiamato cliccando il link di password recovery nell'email.
	 * @return true se ok, false se la request è invalida
	 */
	public boolean passwordResetForm(String token, Model model, RedirectAttributes redirectAttributes) {
		long[] parts = yadaTokenHandler.parseLink(token);
		try {
			if (parts!=null) {
				List<YadaRegistrationRequest> registrationRequests = yadaRegistrationRequestRepository.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]);
				if (registrationRequests.isEmpty()) {
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
		yadaWebUtil.modalError("Password change failed", "An error occurred while changing the password. Please try again and contact us if the problem persists.", redirectAttributes);
		return false;
	}

	@RequestMapping("/passwordRecovery")
	public String passwordRecovery(YadaRegistrationRequest yadaRegistrationRequest, BindingResult bindingResult, Locale locale, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response) {
		if (yadaRegistrationRequest==null || yadaRegistrationRequest.getEmail()==null) {
			return "redirect:/pwdRecover";
		}
		// Controllo se esiste un utente
		String email = yadaRegistrationRequest.getEmail();
		List<YadaUserCredentials> existing = yadaUserCredentialsRepository.findByUsername(StringUtils.trimToEmpty(email).toLowerCase(locale), yadaWebUtil.FIND_ONE);
		if (existing.isEmpty()) {
			bindingResult.rejectValue("email", "passwordrecover.username.notfound");
			return "/pwdRecover";
		}
		yadaRegistrationRequest.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
		// Pulisco le vecchie richieste
		yadaSecurityUtil.registrationRequestCleanup(yadaRegistrationRequest);
		yadaRegistrationRequest.setPassword("fakefake"); // La metto solo per evitare un errore di validazione al save
		yadaRegistrationRequest = yadaRegistrationRequestRepository.save(yadaRegistrationRequest); // Va fatto subito per avere l'id e il token
		boolean emailSent = yadaSecurityEmailService.sendPasswordRecovery(yadaRegistrationRequest, request, locale);
		if (!emailSent) {
			yadaRegistrationRequestRepository.delete(yadaRegistrationRequest);
			log.debug("Invio email a {} fallito in fase di recupero password", yadaRegistrationRequest.getEmail());
			bindingResult.rejectValue("email", "email.send.failed");
			return "/pwdRecover";
		}
		YadaNotify yadaNotify = YadaNotify.instance(redirectAttributes).yadaMessageSource(messageSource, locale);
		yadaNotify.yadaTitleKey("email.passwordrecover.title");
		yadaNotify.yadaOk().yadaMessageKey("email.passwordrecover.message", yadaRegistrationRequest.getEmail());
		yadaNotify.yadaSave();
        return "redirect:/"; 
	}
}
