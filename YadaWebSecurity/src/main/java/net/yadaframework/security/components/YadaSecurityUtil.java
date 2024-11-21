package net.yadaframework.security.components;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaRegistrationRequestDao;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.web.YadaSession;
import net.yadaframework.web.form.YadaFormPasswordChange;

@Component
public class YadaSecurityUtil {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private Date lastOldCleanup = null; // Data dell'ultimo cleanup, ne viene fatto uno al giorno
	private Object lastOldCleanupMonitor = new Object();

	private static final int MAX_AGE_DAY=20; // Tempo dopo il quale una richiesta viene cancellata
	private static final long MILLIS_IN_DAY = 24*60*60*1000; // Millesimi di secondo in un giorno
	private static final String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST"; // copiato da org.springframework.security.web.savedrequest.HttpSessionRequestCache

	private SecureRandom secureRandom = new SecureRandom();

	@Autowired private HttpSession httpSession; // Funziona perchè è un proxy
	@Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaRegistrationRequestDao yadaRegistrationRequestDao;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaConfiguration config;
	@Autowired private PasswordEncoder passwordEncoder; // Null when encoding not configured
	@Autowired private AuthorizationManager<HttpServletRequest> authorizationManager;

	 /**
     * Checks if the current user has access to the specified path
     * @param request The current HttpServletRequest
     * @param path The path to check access for, e.g. "/dashboard"
     * @return true if access is granted, false otherwise
     */
    public boolean checkUrlAccess(HttpServletRequest request, String path) {
    	String normalizedPath = normalizePath(path);
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null) {
                log.debug("No authentication found in context");
                return false;
            }
            HttpServletRequest modifiedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return normalizedPath; 
                }
                @Override
                public String getServletPath() {
                    return normalizedPath;
                }
            };
            AuthorizationDecision decision = authorizationManager.check(() -> authentication, modifiedRequest);
            if (decision == null) {
                log.debug("No security constraints found for path: {}", normalizedPath);
                return true; // No security constraints = permitted
            }
            boolean granted = decision.isGranted();
            if (granted) {
                log.debug("Access granted to path: {} for user: {}", normalizedPath, authentication.getName());
            } else {
                log.debug("Access denied to path: {} for user: {}", normalizedPath, authentication.getName());
            }
            return granted;
        } catch (Exception e) {
            log.error("Error checking URL access for path: {}", normalizedPath, e);
            return false;
        }
    }
    
    private String normalizePath(String path) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex != -1) {
            normalized = normalized.substring(0, queryIndex);
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }   
    
	/**
	 * Check if a user has been suspended for excess of login failures
	 * @param yadaUserProfile
	 * @return true if the user is locked out
	 */
	public boolean isLockedOut(YadaUserProfile yadaUserProfile) {
		YadaUserCredentials userCredentials = yadaUserProfile.getUserCredentials();
		int maxFailed = config.getMaxPasswordFailedAttempts();
		int lockMillis = config.getPasswordFailedAttemptsLockoutMinutes()*60000;
		int failedAttempts = userCredentials.getFailedAttempts();
		Date lastFailedTimestamp = userCredentials.getLastFailedAttempt();
		return failedAttempts>maxFailed && lastFailedTimestamp!=null && System.currentTimeMillis()-lastFailedTimestamp.getTime()<lockMillis;
	}

	/**
	 * Set a new password using the configured encoder. Also sets the password timestamp and clears the failed attempts.
	 * The "force password change" flag is not cleared for the use case of a user being forced to set
	 * a new password after first login with a provided password.
	 * The password encoder is configured with &lt;encodePassword>true&lt;/encodePassword>
	 * <br>
	 * The userProfile is not saved.
	 * @param userProfile
	 * @param newPassword
	 */
	public void changePassword(YadaUserProfile userProfile, String newPassword) {
		YadaUserCredentials userCredentials = userProfile.getUserCredentials();
		if (passwordEncoder!=null) {
			newPassword=passwordEncoder.encode(newPassword);
		}
		userCredentials.setPassword(newPassword);
		userCredentials.setPasswordDate(new Date());
		// NO changePassword = false;
		userCredentials.setFailedAttempts(0);
		userCredentials.setLastFailedAttempt(null);
	}
	
	/**
	 * Ensures that roles set on some target user can be set by the current user.
	 * Permissions to change roles are specified in the &lt;role>&lt;handles> configuration parameter.
	 * @param actingUser the user that wants to change roles on some user
	 * @param rolesBefore the roles that the target user had before they were modified
	 * @param rolesAfter the roles that the target user should have after modification. On exit, the roles that can't be changed are reset to the value in rolesBefore
	 * @return the roles that the actingUser can't change
	 */
	public Set<Integer> setRolesWhenAllowed(YadaUserProfile actingUser, List<Integer> rolesBefore, List<Integer> rolesAfter) {
		Set<Integer> forbiddenRoles = new HashSet<Integer>();
		List<Integer> allRoleIds = config.getRoleIds();
		for (Integer roleId : allRoleIds) {
			boolean hadRoleBefore = rolesBefore.contains(roleId);
			boolean hasRoleAfter = rolesAfter.contains(roleId);
			boolean roleChanged = hadRoleBefore!=hasRoleAfter;
			if (roleChanged && !userCanChangeRole(actingUser, roleId)) {
				// The current user can't edit this role, so reset it
				forbiddenRoles.add(roleId);
				if (hadRoleBefore) {
					rolesAfter.add(roleId);
				} else {
					rolesAfter.remove(roleId);
				}
			}
		}
		return forbiddenRoles;
	}

	/**
	 * Returns true if the given user can change the role targetRoleId on users, based on its own roles
	 * @param actingUser the user that wants to change a role
	 * @param targetRoleId the role that the user wants to change
	 * @return true if actingUser can set or clear the targetRoleId on users, as configured by &lt;handles>
	 */
	public boolean userCanChangeRole(YadaUserProfile actingUser, Integer targetRoleId) {
		List<Integer> actingRoleIds = actingUser.getUserCredentials().getRoles();
		Map<Integer, Set<Integer>> roleIdToRoleChange = config.getRoleIdToRoleChange();
		for (Integer actingRoleId : actingRoleIds) {
			Set<Integer> canChangeIds = roleIdToRoleChange.get(actingRoleId);
			if (canChangeIds.contains(targetRoleId)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the given user can change the role targetRoleId on users, based on its own roles
	 * @param actingUser the user that wants to change a role
	 * @param targetRoleKey the role that the current user wants to change, any case
	 * @return true if actingUser can set or clear the targetRoleId on users, as configured by &lt;handles>
	 */
	public boolean userCanChangeRole(YadaUserProfile actingUser, String targetRoleKey) {
		Integer targetRoleId = config.getRoleId(targetRoleKey);
		return userCanChangeRole(actingUser, targetRoleId);
	}	
	
	/**
	 * Check if the roles of the actingUser allow it to change the targetUser based on its roles, as configured by &lt;handles>
	 * A target user can be changed only when its roles can all be changed by any of the roles of the acting user.
	 * @param actingUser
	 * @param targetUser
	 * @return true if actingUser can edit targetUser, false otherwise
	 */
	public boolean userCanEditUser(YadaUserProfile actingUser, YadaUserProfile targetUser) {
		List<Integer> actingRoleIds = actingUser.getUserCredentials().getRoles();
		List<Integer> targetRoleIds = targetUser.getUserCredentials().getRoles();
		Map<Integer, Set<Integer>> roleIdToRoleChange = config.getRoleIdToRoleChange();
		for (Integer actingRoleId : actingRoleIds) {
			Set<Integer> canChangeIds = roleIdToRoleChange.get(actingRoleId);
			if (canChangeIds.containsAll(targetRoleIds)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Logs out the currently logged-in user
	 * @param request
	 */
	public void logout(HttpServletRequest request) {
		new SecurityContextLogoutHandler().logout(request, null, null);
	}

	/**
	 * Copy all not-null login error parameters to the Model
	 * @param request
	 * @param model
	 */
	public void copyLoginErrorParams(HttpServletRequest request, Model model) {
		List<String> params = YadaAuthenticationFailureHandler.getLoginErrorParams(request);
		for (int i = 0; i < params.size(); i++) {
			String name = params.get(i);
			i++;
			String value = params.get(i);
			model.addAttribute(name, value);
		}
	}

	/**
	 * Add to some url the login error request parameters defined in YadaAuthenticationFailureHandler so that the login modal
	 * can show them.
	 * This method should be used when opening the login modal using an ajax call form a normal page as the result of a previous login error.
	 * Usage example:
	 * 	<pre>
    const loginModalUrl = [[${@yadaSecurityUtil.addLoginErrorParams("__@{/some/loginModal(ajaxForm=false)}__")}]];
	yada.ajax(loginModalUrl);
		</pre>
	 * @param url
	 * @return
	 * @see YadaAuthenticationFailureHandler
	 */
	public String addLoginErrorParams(String url) {
		HttpServletRequest request = yadaWebUtil.getCurrentRequest();
		List<String> params = YadaAuthenticationFailureHandler.getLoginErrorParams(request);
		return yadaWebUtil.enhanceUrl(url, null, params.toArray(new String[params.size()]));
	}

	/**
	 * Generate a 32 characters random password
	 * @return a string like "XFofvGEtBlZIa5sH"
	 */
	public String generateClearPassword() {
		return generateClearPassword(32);
	}

	/**
	 * Generate a random password
	 * @param length password length
	 * @return a string like "XFofvGEtBlZIa5sH"
	 */
	public String generateClearPassword(int length) {
		// http://stackoverflow.com/a/8448493/587641
		return RandomStringUtils.random(length, 0, 0, true, true, null, secureRandom);
	}
	
	/**
	 * Change the user password and log in
	 * @param yadaFormPasswordChange
	 * @return true if password changed and user logged in
	 */
	public boolean performPasswordChange(YadaFormPasswordChange yadaFormPasswordChange) {
		int outcome = performPasswordChange(yadaFormPasswordChange, null);
		return outcome == 0;
	}

	/**
	 * Change the user password and log in after eventually checking that the password is actually different from the previous one
	 * @param yadaFormPasswordChange
	 * @param forceDifferentPassword set to true to force a different password
	 * @return the outcome: 0 = ok, 1 = invalid token, 2 = same password as before, 3 = generic error
	 */
	public int performPasswordChange(YadaFormPasswordChange yadaFormPasswordChange, Boolean forceDifferentPassword) {
		long[] parts = yadaTokenHandler.parseLink(yadaFormPasswordChange.getToken());
		try {
			if (parts!=null && parts.length==2) {
				// The token must be valid to get a registrationRequest from DB
				YadaRegistrationRequest registrationRequest = yadaRegistrationRequestDao.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1], YadaRegistrationRequest.class).get(0);
				if (registrationRequest==null) {
					return 1; // Invalid token
				}
				String username = registrationRequest.getEmail();
				YadaUserCredentials yadaUserCredentials = yadaUserCredentialsDao.findFirstByUsername(StringUtils.trimToEmpty(username).toLowerCase());
				if (yadaUserCredentials!=null) {
					//
					if (Boolean.TRUE.equals(forceDifferentPassword)) {
						String newPassword = yadaFormPasswordChange.getPassword();
						if (yadaUserDetailsService.passwordMatch(newPassword, yadaUserCredentials)) {
							log.debug("Password for user {} not changed because same as old one", username);
							return 2; // Same password as before
						}
					}
					//
					yadaUserCredentials = yadaUserCredentialsDao.changePassword(yadaUserCredentials, yadaFormPasswordChange.getPassword());
					yadaRegistrationRequestDao.delete(registrationRequest);
					if (yadaUserCredentials.isEnabled()) {
						yadaUserDetailsService.authenticateAs(yadaUserCredentials);
					}
					log.info("PASSWORD CHANGE for user='{}'", username);
					return 0; // OK
				}
			}
		} catch (Exception e) {
			log.info("Password change failed", e);
		}
		return 3; // Generic error
	}

	/**
	 *
	 * @return the username of the logged-in user, or null
	 */
	public String getUsername() {
		String username = null;
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth!=null && auth.isAuthenticated()) {
				Object principal = auth.getPrincipal();
				if (principal instanceof UserDetails) {
					username = ((UserDetails)principal).getUsername();
				} else if (principal instanceof String) {
					// When user is authenticated anonymously
					username = principal.toString(); // "anonymousUser"
					if ("anonymousUser".equals(username)) {
						// We don't need it
						username = null;
					}
				} else {
					log.debug("principal class = " + principal.getClass().getName());
				}
			}
		} catch (Exception e) {
			log.error("Can't get username", e);
		}
		return username;
	}

	/**
	 * Check if the current user is authenticated (logged in) not anonymously.
	 * Use in thymeleaf with th:if="${@YadaSecurityUtil.loggedIn()}"
	 * @return
	 */
	@Deprecated // use isLoggedIn() instead
	public boolean loggedIn() {
		return isLoggedIn();
	}

	/**
	 * Check if the current user is authenticated (logged in) not anonymously.
	 * Use in thymeleaf with th:if="${@yadaSecurityUtil.loggedIn}"
	 * @return
	 */
	public boolean isLoggedIn() {
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth!=null && auth.isAuthenticated()) {
				Object principal = auth.getPrincipal();
				return (principal instanceof UserDetails);
			}
		} catch (Exception e) {
			log.error("Can't get Authentication object", e);
		}
		return false;
	}

	/**
	 * Check if the current user is logged in.
	 * Use in thymeleaf with th:if="${@yadaWebUtil.loggedIn(#httpServletRequest)}"
	 * @param request
	 * @return
	 */
	public boolean loggedIn(HttpServletRequest request) {
		return request.getRemoteUser()!=null;
	}

	public void clearAnySavedRequest() {
		httpSession.removeAttribute(SAVED_REQUEST);
	}

	/**
	 * Ritorna la richiesta che era stata salvata da Spring Security prima del login, bloccata perchè l'utente non era autenticato
	 * @return la url originale completa di http://, oppure null se non c'è in sessione
	 */
	public String getSavedRequestUrl() {
		SavedRequest savedRequest = (SavedRequest) httpSession.getAttribute(SAVED_REQUEST);
		if (savedRequest!=null) {
			return savedRequest.getRedirectUrl();
		}
		log.debug("No saved request found in session");
		return null;
	}

	/**
	 * Ritorna uno o l'altro parametro a seconda che l'utente corrente sia autenticato o meno.
	 * @param anonymousValue
	 * @param authenticatedValue
	 * @return
	 */
	public String caseAnonAuth(String anonymousValue, String authenticatedValue) {
		boolean authenticated = false;
		try {
			authenticated = SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof UserDetails;
		} catch (Exception e) {
			log.error("Can't get user principal (ignored)");
		}
		return authenticated ? authenticatedValue : anonymousValue;
	}

	/**
	 * Cancello le registration request vecchie o con lo stesso email e tipo. Se la registrationRequest passata � sul database, non viene cancellata.
	 * @param registrationRequest prototipo di richiesta da cancellare (ne viene usato email e tipo)
	 */
	public void registrationRequestCleanup(YadaRegistrationRequest registrationRequest) {
		Date now = new Date();
		// Cancello registrazioni vecchie. Devo sincronizzare per evitare che la delete fallisca in caso di sovrapposizione di pi� utenti.
		// TODO non potevo fare tutto con una semplice query?!!!
		synchronized (lastOldCleanupMonitor) {
			if (lastOldCleanup==null || now.getTime()-lastOldCleanup.getTime() > YadaUtil.MILLIS_IN_DAY) { // Faccio pulizia ogni 24 ore
				lastOldCleanup = now;
				Date limit = new Date(now.getTime() - MAX_AGE_DAY * YadaUtil.MILLIS_IN_DAY); // Pulisco le righe pi� vecchie di MAX_AGE_DAY giorni
				List<YadaRegistrationRequest> oldRequests = yadaRegistrationRequestDao.findByTimestampBefore(limit);
				if (oldRequests.isEmpty()) {
					log.info("No old RegistrationRequest to delete");
				} else {
					for (YadaRegistrationRequest deletable : oldRequests) {
						yadaRegistrationRequestDao.delete(deletable);
						log.info("Expired RegistrationRequest ({}) deleted", deletable);
					}
				}
			}
			// Cancello la precedente richiesta di registrazione per lo stesso email e stesso tipo
			List<YadaRegistrationRequest> ownRequests = yadaRegistrationRequestDao.findByEmailAndRegistrationType(registrationRequest.getEmail(), registrationRequest.getRegistrationType(), YadaRegistrationRequest.class);
			for (YadaRegistrationRequest deletable : ownRequests) {
				if (deletable.getId()!=registrationRequest.getId()) {
					yadaRegistrationRequestDao.delete(deletable);
					log.info("Previous RegistrationRequest ({}) deleted", deletable);
				}
			}
		}
	}

	/**
	 * Returns Spring-formatted roles, like "ROLE_USER" i.e. prefixed by "ROLE_"
	 * @return
	 */
	public Set<String> getCurrentRoles() {
		Set<String> roles = new HashSet<>();
		try {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth!=null && auth.isAuthenticated()) {
				Object principal = auth.getPrincipal();
				if (principal instanceof UserDetails) {
					for (GrantedAuthority ga : ((UserDetails)principal).getAuthorities()) {
						roles.add(ga.getAuthority());
					}
				}
			}
		} catch (Exception e) {
			log.error("Can't get roles", e);
		}
		return roles;
	}

	/**
	 * Check if the current user has the provided role, case sensitive.
	 * This method finds roles in the current SecurityContextHolder and never goes
	 * to database so it may be better than {@link YadaSession#isCurrentRole(String)}
	 * for time-critical use cases
	 * @param roleToCheck without "ROLE_" prefix
	 * @see {@link YadaSession#isCurrentRole(String)} 
	 */
	public boolean hasCurrentRole(String roleToCheck) {
		Set<String> roles = getCurrentRoles();
		return roles.contains(roleToCheck);
	}

	/**
	 * Check if the current user has any of the provided roles, case sensitive.
	 * This method finds roles in the current SecurityContextHolder and never goes
	 * to database but if an instance of the current YadaUserProfile is available,
	 * the {@link YadaUserProfile#hasAnyRoleId(Integer...)} method may be faster.
	 * @param rolesToCheck without "ROLE_" prefix
	 * @see {@link YadaUserProfile#hasAnyRoleId(Integer...)}
	 * @see #hasAnyRole(String...)
	 * @deprecated use {@link #hasAnyRole(String...)} instead
	 */
	@Deprecated // Better use hasAnyRole which is better named and faster
	public boolean hasCurrentRole(String[] rolesToCheck) {
		Set<String> currentRoles = getCurrentRoles();
		Set<String> requiredRoles = new HashSet<>(Arrays.asList(rolesToCheck));
		return CollectionUtils.containsAny(currentRoles, requiredRoles);
	}
	
	/**
	 * Check if the current user has any of the provided roles, case sensitive.
	 * This method finds roles in the current SecurityContextHolder and never goes
	 * to database but if an instance of the current YadaUserProfile is available,
	 * the {@link YadaUserProfile#hasAnyRoleId(Integer...)} method may be faster.
	 * @param rolesToCheck without "ROLE_" prefix
	 * @see {@link YadaUserProfile#hasAnyRoleId(Integer...)}
	 */
	public boolean hasAnyRole(String ... rolesToCheck) {
		Set<String> currentRoles = getCurrentRoles();
		for (String roleToCheck : rolesToCheck) {
			if (currentRoles.contains(roleToCheck)) {
				return true;
			}
		}
		return false;
		
	}
}
