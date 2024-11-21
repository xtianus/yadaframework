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
import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaDataTableFactory;
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
import net.yadaframework.web.datatables.YadaDataTable;

@Controller
@RequestMapping("/dashboard")
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
	@Autowired private YadaDataTableFactory yadaDataTableFactory;

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
	
	@RequestMapping(value="/userwrite/ajaxAddOrUpdateUserProfile")
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
					yadaNotify.titleKey(model, "user.invitation.error.title").error().messageKey("user.invitation.error.message").add();
				} else {
					yadaNotify.titleKey(model, "user.invitation.ok.title").ok().messageKey("user.invitation.ok.message").add();
				}
			}
			return yadaNotify.titleKey(model, "user.addedit.ok.title").ok().messageKey("user.addedit.ok.message", userProfile.getEmail()).add();
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

	@RequestMapping("/userwrite/ajaxDeleteUserProfile")
	public String ajaxDeleteUserProfile(Long[] id, Model model) {
		// Extra security by checking isManager
		if (userSession.isAdmin() && id!=null && id.length>0) {
			for (Long userProfileId : id) {
				String userProfileIdString = userProfileId!=null?userProfileId.toString():"null";
				try {
					UserProfile userProfile = userProfileDao.find(userProfileId);
					deleteUserProfile(userProfile);
					yadaNotify.titleKey(model, "user.delete.ok.title").messageKey("user.delete.ok.message", userProfile.getEmail()).ok().add();
				} catch (Exception e) {
					log.error("Failed to delete user {}", userProfileId, e);
					yadaNotify.titleKey(model, "user.delete.error.title").messageKey("user.delete.error.message", userProfileIdString).error().add();
				}
			}
		} else {
			log.error("Id missing or current user role not admin");
			yadaNotify.titleKey(model, "user.delete.error.title").messageKey("user.delete.internalError.message").error().add();
		}
		return yadaNotify.getViewName();
	}

	@RequestMapping("/userwrite/ajaxEditUserProfileForm")
	public String ajaxEditUserProfileForm(UserProfile userProfile) {
		return "/dashboard/userProfileEditForm";
	}
	
	@RequestMapping(value ="/user/userProfileTablePage", produces = MediaType.APPLICATION_JSON_VALUE)
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
	
	@RequestMapping(value="/user/deimpersonate")
	public String deimpersonate(RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response, Locale locale) {
		String previousLocation = userSession.deimpersonate(request, response);
		if (previousLocation==null) {
			previousLocation = "/";
		}
		return yadaWebUtil.redirectString(previousLocation, locale);
	}

	@RequestMapping(value="/user/impersonate")
	public String impersonate(String id, @RequestParam(name = "currentLocation", required = false) String currentLocation, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response, Locale locale){
		return impersonate(Long.parseLong(id), currentLocation, redirectAttributes, request, response, locale);
	}

	@RequestMapping("/user/impersonate/{id}")
	public String impersonate(@PathVariable long id, @RequestParam(name = "currentLocation", required = false) String currentLocation, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response, Locale locale) {
		UserProfile theUser = userProfileDao.find(id);
		if (theUser==null) {
			yadaNotify.titleKey(redirectAttributes, "user.impersonate.error.title").error().messageKey("user.impersonate.error.message").add();
		} else {
			userSession.impersonate(theUser.getId(), currentLocation, request, response);
			yadaNotify.titleKey(redirectAttributes, "user.impersonate.ok.title").ok().messageKey("user.impersonate.ok.message", theUser.getEmail()).add();
		}
		// model.addAttribute(YadaViews.AJAX_REDIRECT_URL, "/user/dashboard");
		return yadaWebUtil.redirectString("/", locale);
	}
	
	@RequestMapping("/user")
	public String users(Model model, Locale locale) {
		//////////////////////////////
		// Basic example with defaults
		YadaDataTable basicTable = yadaDataTableFactory.getSingleton("basicTable", table -> {
			table
				.dtEntityClass(UserProfile.class)
				.dtStructureObj()
					.dtColumnObj("Email", "userCredentials.username").back()
					.dtColumnObj("Last Login", "userCredentials.lastSuccessfulLogin").back()
				.back();
		});
		model.addAttribute("basicTable", basicTable);
		
		///////////////////
		// Advanced example
		YadaDataTable yadaDataTable = yadaDataTableFactory.getSingleton("userTable", locale, table -> {
			table
				.dtEntityClass(UserProfile.class)
				.dtAjaxUrl("/dashboard/user/userProfileTablePage")
				.dtLanguageObj("/static/datatables-2.1.8/i18n/").dsAddLanguage("pt", "pt-PT.json").back()
				.dtStructureObj()
					.dtCssClasses("yadaNoLoader")
					.dtColumnObj("ID", "id").dtResponsivePriority(80).back()
					// Example of localized text: .dtColumnObj("Title", "title."+locale.getLanguage()).back()
					.dtColumnObj("column.enabled", "userCredentials.enabled").dtResponsivePriority(40).back()
					.dtColumnObj("Nickname", "nickname").dtResponsivePriority(30).back()
					.dtColumnObj("Email", "userCredentials.username").dtName("userCredentials.username").dtOrderAsc(0).dtResponsivePriority(20).back()
					.dtColumnObj("Last Login", "userCredentials.lastSuccessfulLogin").dtOrderDesc(1).dtCssClasses("nowrap").back()
					.dtColumnCheckbox("select.allnone") 
					.dtColumnCommands("column.commands", 10)
					.dtButtonObj("Disabled").dtUrl("@{/dashboard/user/dummy}").dtIcon("<i class='bi bi-0-circle'></i>").dtShowCommandIcon("disableCommandIcon").back()
					.dtButtonObj("button.add").dtUrl("@{/dashboard/userwrite/ajaxEditUserProfileForm}").dtGlobal().dtIcon("<i class='bi bi-plus-square'></i>").dtToolbarCssClass("btn-success").dtRole("ADMIN").back()
					.dtButtonObj("button.impersonate").dtUrlProvider("impersonate").dtNoAjax().dtIcon("<i class='bi bi-mortarboard'></i>").dtRole("ADMIN").dtRole("supervisor").back()
					.dtButtonObj("button.edit").dtUrl("@{/dashboard/userwrite/ajaxEditUserProfileForm}").dtElementLoader("#userTable").dtIcon("<i class='bi bi-pencil'></i>").dtIdName("userProfileId").dtRole("ADMIN").back()
					.dtButtonObj("button.delete").dtUrl("@{/dashboard/userwrite/ajaxDeleteUserProfile}").dtIcon("<i class='bi bi-trash'></i>")
						.dtRole("ADMIN").dtMultiRow().dtToolbarCssClass("btn-danger")
						.dtConfirmDialogObj()
							.dtTitle("Delete User")
							.dtMessageSingular("usertable.delete.confirm.singular")
							.dtMessagePlural("usertable.delete.confirm.plural")
							.dtConfirmButton("button.confirm").dtAbortButton("modal.confirm.cancel")
							.dtPlaceholderColumnName("userCredentials.username")
							.back()
						.back()
					//.dtFooter()
					.back()
				.dtOptionsObj()
					// wrong: .dtLanguageObj().dtUrl("/static/datatables-2.1.8/i18n/" + locale.getLanguage() + ".json").back()
					.dtResponsiveObj()
						.dtDetailsObj()
							.dtDisplay("DataTable.Responsive.display.childRowImmediate")
							.back()
						.back()
					.dtPageLength(10)
					.dtColumnDefsObj().dtTargetsName("userCredentials.username").dtAriaTitle("This is the user email").back()
					.dtColumnDefsObj().dtTargetsName("userCredentials.lastSuccessfulLogin").dtAriaTitle("usertable.aria.lastlogin").back()
					.back()
				;
		});
		model.addAttribute("userTableAttribute", yadaDataTable);
		return "/dashboard/users";
	}
	
	@RequestMapping("/user/legacy")
	public String legacy() {
		return "/dashboard/usersLegacy";
	}

}
