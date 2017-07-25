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
import net.yadaframework.security.persistence.repository.YadaRegistrationRequestRepository;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsRepository;
import net.yadaframework.web.form.YadaFormPasswordChange;

@Controller
public class YadaRegistrationController {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired private YadaUserCredentialsRepository userCredentialsRepository;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	@Autowired private YadaRegistrationRequestRepository registrationRequestRepository;
	@Autowired private YadaSecurityEmailService yadaSecurityEmailService;
	@Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private YadaConfiguration yadaConfiguration;
	@Autowired private MessageSource messageSource;


	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////// Recupero Password                                                                            /////
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
				YadaRegistrationRequest registrationRequest = registrationRequestRepository.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]).get(0);
				YadaUserCredentials yadaUserCredentials = null;
				String username = registrationRequest.getEmail();
				List<YadaUserCredentials> credentials = userCredentialsRepository.findByUsername(StringUtils.trimToEmpty(username).toLowerCase(locale), yadaWebUtil.FIND_ONE);
				if (credentials.size()==1) {
					yadaUserCredentials = credentials.get(0);
					yadaUserCredentials.changePassword(yadaFormPasswordChange.getPassword(), passwordEncoder);
					userCredentialsRepository.save(yadaUserCredentials);
					registrationRequestRepository.delete(registrationRequest);
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
				List<YadaRegistrationRequest> registrationRequests = registrationRequestRepository.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]);
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
		List<YadaUserCredentials> existing = userCredentialsRepository.findByUsername(StringUtils.trimToEmpty(email).toLowerCase(locale), yadaWebUtil.FIND_ONE);
		if (existing.isEmpty()) {
			bindingResult.rejectValue("email", "passwordrecover.username.notfound");
			return "/pwdRecover";
		}
		yadaRegistrationRequest.setRegistrationType(YadaRegistrationType.PASSWORD_RECOVERY);
		// Pulisco le vecchie richieste
		yadaSecurityUtil.registrationRequestCleanup(yadaRegistrationRequest);
		yadaRegistrationRequest.setPassword("fakefake"); // La metto solo per evitare un errore di validazione al save
		yadaRegistrationRequest = registrationRequestRepository.save(yadaRegistrationRequest); // Va fatto subito per avere l'id e il token
		boolean emailSent = yadaSecurityEmailService.sendPasswordRecovery(yadaRegistrationRequest, request, locale);
		if (!emailSent) {
			registrationRequestRepository.delete(yadaRegistrationRequest);
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
