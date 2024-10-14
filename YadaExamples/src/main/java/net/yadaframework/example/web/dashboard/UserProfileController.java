package net.yadaframework.example.web.dashboard;

import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import net.yadaframework.components.YadaNotify;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.example.components.YexEmailService;
import net.yadaframework.example.core.YexConfiguration;
import net.yadaframework.example.persistence.entity.UserProfile;
import net.yadaframework.example.persistence.repository.UserProfileDao;
import net.yadaframework.example.web.UserSession;
import net.yadaframework.persistence.YadaDataTableDao;
import net.yadaframework.persistence.YadaSql;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.web.YadaDatatablesRequest;
import net.yadaframework.web.YadaViews;

@Controller
@RequestMapping("/dashboard/user")
public class UserProfileController {
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired private YadaDataTableDao yadaDataTableDao;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaNotify yadaNotify;
	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private UserProfileDao userProfileDao;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private YexConfiguration config;
	@Autowired private YexEmailService yexEmailService;
	@Autowired private UserSession userSession;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;

	@ModelAttribute("userProfile")
	public UserProfile addUserProfile(@RequestParam(value="userProfileId", required=false) Long id) {
		UserProfile toEdit = null;
		Exception exception = null;
		if (id!=null) {
			try {
				toEdit = userProfileDao.find(id);
			} catch (Exception e) {
				exception = e;
			}
			if (toEdit==null) {
				log.error("Can't find UserProfile with id={} - (creating new)", id, exception);
			} else {
				log.debug("UserProfile {}-{} fetched from DB as ModelAttribute", id, toEdit.getEmail());
			}
		}
		if (toEdit==null) {
			toEdit = new UserProfile();
			YadaUserCredentials yadaUserCredentials = new YadaUserCredentials();
			yadaUserCredentials.setNewPassword(yadaSecurityUtil.generateClearPassword(8));
			yadaUserCredentials.setChangePassword(true);
			yadaUserCredentials.setEnabled(true);
			toEdit.setUserCredentials(yadaUserCredentials);
		}
		return toEdit;
	}
	
	@RequestMapping(value="/ajaxAddOrUpdateUserProfile", params={"yadaconfirmed"})
	public String ajaxAddOrUpdateUserProfileConfirmed(UserProfile userProfile, BindingResult userProfileBinding, Model model) {
		boolean inviteEmail = userProfile.isInviteEmail(); // Save it before is overwritten
		ValidationUtils.rejectIfEmptyOrWhitespace(userProfileBinding, "userCredentials.username", "validation.value.empty");
		ValidationUtils.rejectIfEmpty(userProfileBinding, "userCredentials.roles", "validation.value.empty");

		// Controllo i casi in cui l'username esista già:
		// - ne esiste già uno con un id diverso da quello arrivato via web
		YadaUserCredentials existing = yadaUserCredentialsDao.findFirstByUsername(userProfile.getEmail());
		if (existing!=null && !existing.getId().equals(userProfile.getUserCredentials().getId())) {
			userProfileBinding.rejectValue("userCredentials.username", "validation.value.existing");
		}
		if (!userProfileBinding.hasErrors()) {
			YadaUserCredentials yadaUserCredentials = userProfile.getUserCredentials();
			String username = yadaUserCredentials.getUsername().trim();
			String newPassword = StringUtils.trimToNull(yadaUserCredentials.getNewPassword());
			yadaUserCredentials.setUsername(username); // Trimmed
			if (newPassword!=null) {
				yadaUserCredentials.changePassword(newPassword, passwordEncoder);
			}
			userProfile = userProfileDao.save(userProfile);
			
			// Send invitation email
			if (inviteEmail) {
				HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
				Locale locale = userProfile.getLocale();
				boolean invited = yexEmailService.sendInvitation(userProfile, newPassword, request, locale);
				if (!invited) {
					yadaNotify.title("Email non inviata", model).error().message("Errore durante l'invio della email di invito").add();
				} else {
					yadaNotify.title("Email inviata", model).ok().message("Email di invito inviata con successo").add();
				}
			}
			
			yadaNotify.title("Salvataggio completato", model).ok().message("Utente " + userProfile.getEmail() + " memorizzato").add();
			return YadaViews.AJAX_NOTIFY;
		}
		return ajaxEditUserProfileForm(userProfile);
	}

	/**
	 * Delete a user
	 * @param userProfile
	 */
	public void deleteUserProfile(UserProfile userProfile) {
		// Delete the user
		userProfileDao.delete(userProfile);
		userSession.clearCaches();
	}

	@RequestMapping("/ajaxDeleteUserProfile")
	public String ajaxDeleteUserProfile(Long[] id, Model model) {
		// Extra security by checking isManager
		if (userSession.isAdmin() && id!=null && id.length>0) {
			for (Long userProfileId : id) {
				try {
					UserProfile userProfile = userProfileDao.find(userProfileId);
					deleteUserProfile(userProfile);
					yadaNotify.title("Cancellazione Eseguita", model).message("Utente \"{}\" eliminato", userProfile.getEmail()).ok().add();
				} catch (Exception e) {
					log.error("Delete dell'utente {} fallito", userProfileId, e);
					yadaNotify.title("Cancellazione Fallita", model).message("L'eliminazione non può essere completata").error().add();
				}
			}
		} else {
			yadaNotify.title("Cancellazione Fallita", model).message("Errore interno").error().add();
		}
		return YadaViews.AJAX_NOTIFY;
	}

	@RequestMapping("/ajaxEditUserProfileForm")
	public String ajaxEditUserProfileForm(UserProfile userProfile) {
		return "/dashboard/userProfileEditForm";
	}
	
	@RequestMapping(value ="/userProfileTablePage", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public Map<String, Object> userProfileTablePage(YadaDatatablesRequest yadaDatatablesRequest, Locale locale) {
//		boolean usersSuspended = yadaDatatablesRequest.getExtraParam().get("usersSuspendedSet")!=null;
		YadaSql yadaSql = yadaDatatablesRequest.getYadaSql();
//		yadaSql.where(usersSuspended,":suspendedRole MEMBER OF e.userCredentials.roles") //.and()
//				.where(!usersSuspended,":suspendedRole NOT MEMBER OF e.userCredentials.roles") //.and()
//				.setParameter("suspendedRole", VlbUtil.ROLE_SUSPENDED_ID);

		// yadaDatatablesRequest.addExtraJsonAttribute("enabled");
		// yadaDatatablesRequest.addExtraJsonAttribute("registrationDate");
		// yadaDatatablesRequest.addExtraJsonAttribute("email");
		// yadaDatatablesRequest.addExtraJsonAttribute("loginDate");
		Map<String, Object> result = yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, UserProfile.class, locale);
		return result;
	}
	
	@RequestMapping(value="/deimpersonate")
	public String deimpersonate(RedirectAttributes redirectAttributes){
		userSession.depersonate();
		return "redirect:/";
	}

	// Impersonamento
	@RequestMapping(value="/impersonate")
	public String impersonate(String id, RedirectAttributes redirectAttributes, Locale locale){
		return impersonate(Long.parseLong(id), redirectAttributes, locale);
	}

	// Impersonamento
	@RequestMapping("/impersonate/{id}")
	public String impersonate(@PathVariable long id, RedirectAttributes redirectAttributes, Locale locale) {
		UserProfile theUser = userProfileDao.find(id);
		if (theUser==null) {
			yadaNotify.title("Impersonation failed", redirectAttributes).error().message("L'utente che vuoi impersonare non esiste").add();
		} else {
			userSession.impersonate(theUser.getId());
			yadaNotify.title("Impersonation On", redirectAttributes).ok().message("Stai operando come " + theUser.getEmail()).add();
		}
		// model.addAttribute(YadaViews.AJAX_REDIRECT_URL, "/user/dashboard");
		return yadaWebUtil.redirectString("/", locale);
	}
	
	@RequestMapping("")
	public String users() {
		return "/dashboard/users";
	}
	
	@RequestMapping("/legacy")
	public String legacy() {
		return "/dashboard/usersLegacy";
	}

}
