package net.yadaframework.example.web.dashboard;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import net.yadaframework.persistence.YadaDataTableQueryPolicy;
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

	/**
	 * The only request parameters that the add/edit user form may bind. The failed attempts counter is not bound: it
	 * can only be reset, see {@link #ajaxAddOrUpdateUserProfileConfirmed}.
	 */
	private static final String[] EDITABLE_FIELDS = { "userCredentials.username", "userCredentials.enabled", "userCredentials.roles", "userCredentials.changePassword", "userCredentials.newPassword", "inviteEmail" };

	private enum EditOutcomeEnum { SAVED, MISSING, FORBIDDEN, INVALID }

	/**
	 * The add/edit user form binds a new, never persisted UserProfile and only the editable fields: ids, versions,
	 * password hashes, timestamps and relationships can't be set from the request.
	 */
	@InitBinder("userProfile")
	public void initUserProfileBinder(WebDataBinder binder) {
		binder.setAllowedFields(EDITABLE_FIELDS);
	}

	/**
	 * Adds a user when userProfileId is missing, otherwise edits that user. The profile and its credentials are loaded
	 * from the database by userProfileId alone; the stored and the requested roles are authorized before the submitted
	 * fields are copied onto them in one transaction.
	 * @param userProfileId the user to edit, null to add a user
	 * @param editedUserProfile the submitted form, holding only the {@link #EDITABLE_FIELDS}
	 * @param failedAttempts optional: 0 resets the failed login attempts of the user, any other change is rejected
	 */
	@PostMapping("/userwrite/ajaxAddOrUpdateUserProfile")
	public String ajaxAddOrUpdateUserProfileConfirmed(Long userProfileId, @ModelAttribute("userProfile") UserProfile editedUserProfile, BindingResult userProfileBinding, @RequestParam(name = "userCredentials.failedAttempts", required = false) Integer failedAttempts, Model model, Locale locale) {
		boolean inviteEmail = userProfileId==null && editedUserProfile.isInviteEmail();
		YadaUserCredentials submittedCredentials = editedUserProfile.getUserCredentials();
		if (submittedCredentials==null) {
			submittedCredentials = new YadaUserCredentials();
			editedUserProfile.setUserCredentials(submittedCredentials);
		}
		// The "user" role is forced
		submittedCredentials.ensureRole(config.getRoleId("user"));
		// Validation
		ValidationUtils.rejectIfEmptyOrWhitespace(userProfileBinding, "userCredentials.username", "validation.value.empty");
		ValidationUtils.rejectIfEmpty(userProfileBinding, "userCredentials.roles", "validation.value.empty");
		String newPassword = StringUtils.trimToNull(submittedCredentials.getNewPassword());
		UserProfile currentUserProfile = userSession.getCurrentUserProfile();
		UserProfile savedUserProfile;
		EditOutcomeEnum outcome;
		if (userProfileId==null) {
			if (newPassword==null) {
				newPassword = yadaSecurityUtil.generateClearPassword(8);
			}
			savedUserProfile = new UserProfile();
			savedUserProfile.setUserCredentials(new YadaUserCredentials());
			outcome = applyEdit(currentUserProfile, savedUserProfile, editedUserProfile, newPassword, failedAttempts, userProfileBinding);
			if (outcome==EditOutcomeEnum.SAVED) {
				savedUserProfile = userProfileDao.save(savedUserProfile);
			}
		} else {
			final String password = newPassword;
			outcome = userProfileDao.editInTransaction(userProfileId, stored -> stored==null ? EditOutcomeEnum.MISSING : applyEdit(currentUserProfile, stored, editedUserProfile, password, failedAttempts, userProfileBinding));
			savedUserProfile = userProfileDao.find(userProfileId);
		}
		switch (outcome) {
		case MISSING:
			log.warn("Can't edit missing UserProfile with id={}", userProfileId);
			return yadaNotify.titleKey(model, "user.edit.error.title").error().messageKey("user.edit.missing.message").add();
		case FORBIDDEN:
			if (userProfileId==null) {
				return yadaNotify.titleKey(model, "user.add.error.title").error().messageKey("user.add.forbidden.message").add();
			}
			return yadaNotify.titleKey(model, "user.edit.error.title").error().messageKey("user.edit.forbidden.message", String.valueOf(savedUserProfile)).add();
		case INVALID:
			if (userProfileId!=null) {
				// Values shown but not editable in the form
				editedUserProfile.setId(savedUserProfile.getId());
				editedUserProfile.setNickname(savedUserProfile.getNickname());
				submittedCredentials.setCreationDate(savedUserProfile.getUserCredentials().getCreationDate());
				submittedCredentials.setLastSuccessfulLogin(savedUserProfile.getUserCredentials().getLastSuccessfulLogin());
				submittedCredentials.setFailedAttempts(failedAttempts!=null ? failedAttempts : savedUserProfile.getUserCredentials().getFailedAttempts());
			}
			return ajaxEditUserProfileForm(editedUserProfile, model);
		default:
			break;
		}

		// Send invitation email
		if (inviteEmail) {
			HttpServletRequest request = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
			Locale invitationLocale = currentUserProfile.getLocale()!=null ? currentUserProfile.getLocale() : locale; // Eventually should be a choice in the add form
			boolean invited = yexEmailService.sendInvitation(savedUserProfile, newPassword, request, invitationLocale);
			if (!invited) {
				yadaNotify.titleKey(model, "user.invitation.error.title").error().messageKey("user.invitation.error.message").add();
			} else {
				yadaNotify.titleKey(model, "user.invitation.ok.title").ok().messageKey("user.invitation.ok.message").add();
			}
		}
		return yadaNotify.titleKey(model, "user.addedit.ok.title").ok().messageKey("user.addedit.ok.message", savedUserProfile.getEmail()).add();
	}

	/**
	 * Authorizes the stored and the requested roles, validates the submitted values and only then copies the editable
	 * fields onto the stored user. Nothing is changed unless the result is SAVED.
	 * @param actingUser the current user
	 * @param stored the user to change: loaded from the database, or new
	 * @param submitted the bound form
	 * @param newPassword the new clear password, or null to keep the current one
	 * @param failedAttempts the submitted failed attempts counter, or null
	 */
	private EditOutcomeEnum applyEdit(UserProfile actingUser, UserProfile stored, UserProfile submitted, String newPassword, Integer failedAttempts, BindingResult binding) {
		YadaUserCredentials storedCredentials = stored.getUserCredentials();
		YadaUserCredentials submittedCredentials = submitted.getUserCredentials();
		List<Integer> oldRoles = stored.getId()!=null ? new ArrayList<>(storedCredentials.getRoles()) : null;
		List<Integer> newRoles = submittedCredentials.getRoles();
		if (!yadaSecurityUtil.userCanEditUser(actingUser, oldRoles, newRoles)) {
			return EditOutcomeEnum.FORBIDDEN;
		}
		String username = StringUtils.trimToNull(submittedCredentials.getUsername());
		// Check if username already exists with a different id
		YadaUserCredentials existing = username!=null ? yadaUserCredentialsDao.findFirstByUsername(username) : null;
		if (existing!=null && !existing.getId().equals(storedCredentials.getId())) {
			binding.rejectValue("userCredentials.username", "validation.value.existing");
		}
		// The only change allowed to the failed attempts is a reset, to unlock the user
		boolean resetFailedAttempts = failedAttempts!=null && failedAttempts==0 && storedCredentials.getFailedAttempts()>0;
		if (failedAttempts!=null && failedAttempts!=0 && failedAttempts!=storedCredentials.getFailedAttempts()) {
			binding.rejectValue("userCredentials.failedAttempts", "validation.failedAttempts.resetOnly");
		}
		if (binding.hasErrors()) {
			return EditOutcomeEnum.INVALID;
		}
		storedCredentials.setUsername(username);
		storedCredentials.setEnabled(submittedCredentials.isEnabled());
		storedCredentials.setChangePassword(submittedCredentials.isChangePassword());
		if (storedCredentials.getRoles()==null) {
			storedCredentials.setRoles(new ArrayList<>());
		}
		storedCredentials.getRoles().clear();
		storedCredentials.getRoles().addAll(newRoles);
		if (newPassword!=null) {
			yadaSecurityUtil.changePassword(stored, newPassword); // Also resets the failed attempts
		}
		if (resetFailedAttempts) {
			storedCredentials.setFailedAttempts(0);
			storedCredentials.setLastFailedAttempt(null);
		}
		return EditOutcomeEnum.SAVED;
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

	/**
	 * The add/edit user form
	 * @param userProfileId the user to edit, null to add a user
	 */
	@RequestMapping("/userwrite/ajaxEditUserProfileForm")
	public String ajaxEditUserProfileForm(Long userProfileId, Model model) {
		UserProfile userProfile;
		if (userProfileId!=null) {
			userProfile = userProfileDao.find(userProfileId);
			if (userProfile==null) {
				log.warn("Can't find UserProfile with id={}", userProfileId);
				return yadaNotify.titleKey(model, "user.edit.error.title").error().messageKey("user.edit.missing.message").add();
			}
		} else {
			userProfile = new UserProfile();
			YadaUserCredentials yadaUserCredentials = new YadaUserCredentials();
			yadaUserCredentials.setNewPassword(yadaSecurityUtil.generateClearPassword(8));
			yadaUserCredentials.setChangePassword(true);
			yadaUserCredentials.setEnabled(true);
			userProfile.setUserCredentials(yadaUserCredentials);
		}
		return ajaxEditUserProfileForm(userProfile, model);
	}

	private String ajaxEditUserProfileForm(UserProfile userProfile, Model model) {
		model.addAttribute("userProfile", userProfile);
		return "/dashboard/userProfileEditForm";
	}
	
	/**
	 * What userProfileTablePage may query and return: the columns of both the "userTable" and the legacy "usersTable".
	 * Anything else that the browser asks for is rejected.
	 */
	private static final YadaDataTableQueryPolicy USER_TABLE_POLICY = YadaDataTableQueryPolicy.forEntity(UserProfile.class)
		.column("id")
		.column("userCredentials.enabled")
		.column("nickname")
		.column("userCredentials.username")
		.column("userCredentials.lastSuccessfulLogin")
		.build();

	@RequestMapping(value ="/user/userProfileTablePage", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody public Map<String, Object> userProfileTablePage(YadaDatatablesRequest yadaDatatablesRequest, Locale locale) {//		boolean usersSuspended = yadaDatatablesRequest.getExtraParam().get("usersSuspendedSet")!=null;
	YadaSql yadaSql = yadaDatatablesRequest.getYadaSql();
//		yadaSql.where(usersSuspended,":suspendedRole MEMBER OF e.userCredentials.roles") //.and()
//				.where(!usersSuspended,":suspendedRole NOT MEMBER OF e.userCredentials.roles") //.and()
//				.setParameter("suspendedRole", VlbUtil.ROLE_SUSPENDED_ID);

	// yadaDatatablesRequest.addExtraJsonAttribute("enabled");
	// yadaDatatablesRequest.addExtraJsonAttribute("registrationDate");
	// yadaDatatablesRequest.addExtraJsonAttribute("email");
	// yadaDatatablesRequest.addExtraJsonAttribute("loginDate");
	
	// Role filter
	Map<String,String> extraParam = yadaDatatablesRequest.getExtraParam();
	if (extraParam!=null) {
		String roleKeys = extraParam.get("rolekey");
		if (roleKeys!=null) {
			String[] roleKeyArray = roleKeys.split(",");
			for (int i = 0; i < roleKeyArray.length; i++) {
			    String roleKey = roleKeyArray[i];
			    int roleId = config.getRoleId(roleKey);
				yadaSql.where("where :role"+i+" member of userCredentials.roles").and();
				yadaSql.setParameter("role"+i, roleId);
			}
		}
	}
	
	Map<String, Object> result = yadaDataTableDao.getConvertedJsonPage(yadaDatatablesRequest, UserProfile.class, locale, USER_TABLE_POLICY);
		return result;
	}
	
	/**
	 * Ends the impersonation and goes back to the local path where it was started, or to the home page
	 */
	@PostMapping("/user/deimpersonate")
	public String deimpersonate(HttpServletRequest request, HttpServletResponse response) {
		changeSessionId(request);
		String previousLocation = userSession.deimpersonate(request, response);
		// Validated again because it could have been saved by other code
		return "redirect:" + yadaWebUtil.safeLocalRedirect(previousLocation);
	}

	@PostMapping("/user/impersonate")
	public String impersonate(String id, String currentLocation, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response, Locale locale){
		return impersonate(Long.parseLong(id), currentLocation, redirectAttributes, request, response, locale);
	}

	/**
	 * Starts impersonating a user that the current user is allowed to impersonate
	 * @param id the user to impersonate
	 * @param currentLocation the local path to return to when the impersonation ends; any other value is replaced by "/"
	 */
	@PostMapping("/user/impersonate/{id}")
	public String impersonate(@PathVariable long id, String currentLocation, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpServletResponse response, Locale locale) {
		UserProfile theUser = userProfileDao.find(id);
		if (theUser==null) {
			yadaNotify.titleKey(redirectAttributes, "user.impersonate.error.title").error().messageKey("user.impersonate.missing.message").add();
		} else {
			// Check role permissions
			if (!yadaSecurityUtil.userCanImpersonate(userSession.getCurrentUserProfile(), theUser)) {
				yadaNotify.titleKey(redirectAttributes, "user.impersonate.error.title").error().messageKey("user.impersonate.noauth.message").add();
			} else {
				changeSessionId(request);
				userSession.impersonate(theUser.getId(), yadaWebUtil.safeLocalRedirect(currentLocation), request, response);
				yadaNotify.titleKey(redirectAttributes, "user.impersonate.ok.title").ok().messageKey("user.impersonate.ok.message", theUser.getEmail()).add();
			}
		}
		// model.addAttribute(YadaViews.AJAX_REDIRECT_URL, "/user/dashboard");
		return yadaWebUtil.redirectString("/", locale);
	}

	/**
	 * A new session id when the authenticated user changes, against session fixation
	 */
	private void changeSessionId(HttpServletRequest request) {
		try {
			request.changeSessionId();
		} catch (IllegalStateException e) {
			log.debug("No session to change at impersonation (ignored)");
		}
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
				// .dtAjaxUrl("/dashboard/user/userProfileTablePage")
				.dtAjaxUrl(this::userProfileTablePage)
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
					.dtButtonObj("button.add").dtUrl("@{/dashboard/userwrite/ajaxEditUserProfileForm}").dtGlobal().dtIcon("<i class='bi bi-plus-square'></i>").dtToolbarCssClass("btn-success").back()
					.dtButtonObj("button.impersonate").dtUrlProvider("impersonate").dtNoAjax().dtIcon("<i class='bi bi-mortarboard'></i>").dtRole("ADMIN").dtRole("supervisor").back()
					.dtButtonObj("button.edit").dtUrl("@{/dashboard/userwrite/ajaxEditUserProfileForm}").dtElementLoader("#userTable").dtIcon("<i class='bi bi-pencil'></i>").dtIdName("userProfileId").back()
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
