package net.yadaframework.security.web;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.components.YadaUserDetailsService;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.security.persistence.repository.YadaUserProfileDao;
import net.yadaframework.web.YadaCropQueue;

/**
 * Base class for application session. The subclass must be annotated with "@Primary" otherwise two different instances are created for the two classes
 */
@Component
@SessionScope
// NOTE: Use @Primary in subclasses
public class YadaSession<T extends YadaUserProfile> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	@Autowired protected YadaConfiguration config;
	@Autowired protected YadaSecurityUtil yadaSecurityUtil;
	// Attenzione: questo sarebbe da mettere transient perchè tomcat tenta di persistere la session ma non ce la fa. Pero' se lo si mette transient,
	// quando la session viene ricaricata questo non viene valorizzato. Come si fa a inizializzare questo oggetto quando tomcat lo ricarica dallo storage?
	@Autowired protected YadaUserProfileDao<T> yadaUserProfileDao;
	@Autowired protected YadaUserDetailsService yadaUserDetailsService;
	@Autowired protected YadaUserCredentialsDao yadaUserCredentialsDao;
	// @Autowired protected YadaFileManagerDao yadaFileManagerDao;
	@Autowired protected YadaUtil yadaUtil;

	protected Long impersonatorUserId = null;
	protected Long impersonatedUserId = null;
	protected Long loggedInUserProfileId = null; // id of the current logged in user, be it "real" or "impersonated"
	protected String impersonationStartingLocation = null;

	protected YadaCropQueue cropQueue; // Crop images

	public void clearUserProfileCache() {
		loggedInUserProfileId = null;
	}

	public void clearCaches() {
		clearUserProfileCache();
		impersonatorUserId = null;
		impersonatedUserId = null;
		impersonationStartingLocation = null;
	}

	@Deprecated // use isImpersonationActive()
	public boolean isImpersonificationActive() {
		return isImpersonationActive();
	}

	public boolean isImpersonationActive() {
		return impersonatorUserId!=null && impersonatedUserId!=null;
	}

	/**
	 * Assume the identity of the given user. Deprecated: use impersonate()
	 * @param targetUserProfileId
	 */
	@Deprecated // use impersonate()
	public void impersonify(Long targetUserProfileId) {
		impersonate(targetUserProfileId);
	}

	/**
	 * Assume the identity of the given user
	 * @param targetUserProfileId
	 */
	public void impersonate(Long targetUserProfileId) {
		impersonate(targetUserProfileId, null, null, null);
	}
	
	/**
	 * Assume the identity of the given user
	 * @param targetUserProfileId
	 */
	public void impersonate(Long targetUserProfileId, String currentLocation, HttpServletRequest request, HttpServletResponse response) {
		impersonatorUserId = getCurrentUserProfileId();
		impersonatedUserId = targetUserProfileId;
		YadaUserCredentials targetUserCredentials = yadaUserCredentialsDao.findByUserProfileId(targetUserProfileId);
		yadaUserDetailsService.authenticateAs(targetUserCredentials, false, request, response);
		loggedInUserProfileId = targetUserProfileId;
		impersonationStartingLocation = currentLocation;
		log.info("Impersonation by #{} as #{} started", impersonatorUserId, targetUserCredentials.getId());
	}

	/**
	 * @deprecated Use {@link #deimpersonate()} instead.
	 */
	@Deprecated
	public boolean depersonify() {
		return depersonate();
	}
	
	/**
	 * @deprecated Use {@link #deimpersonate()} instead.
	 */
	@Deprecated
	public boolean depersonate() {
		deimpersonate(null, null);
		return true; 
	}

	/**
	 * @deprecated Use {@link #deimpersonate(HttpServletRequest, HttpServletResponse) instead
	 */
	@Deprecated
	public boolean depersonate(HttpServletRequest request, HttpServletResponse response) {
		deimpersonate(request, response);
		return true; 
	}
	
	/**
	 * Terminates impersonation.
	 * @return the browser location where impersonation was started, if saved, null otherwise.
	 */
	public String deimpersonate(HttpServletRequest request, HttpServletResponse response) {
		String result = null;
		if (isImpersonationActive()) {
			result = impersonationStartingLocation;
			YadaUserCredentials originalCredentials = yadaUserCredentialsDao.findByUserProfileId(impersonatorUserId);
			yadaUserDetailsService.authenticateAs(originalCredentials, request, response);
			log.info("Impersonation by #{} ended", originalCredentials.getId());
			clearCaches();
			loggedInUserProfileId = impersonatorUserId;
		} else {
			log.error("Deimpersonation failed because of null impersonator or null original user");
		}
		return result;
	}

	/**
	 * Check if the current logged in user (if any) has the specified role.
	 * This method must load the current user profile from database when not already
	 * cached in the user session, so it might be slower than {@link YadaSecurityUtil#hasCurrentRole(String)}
	 * for time-critical use cases. 
	 * @param roleString the role name, case insensitive, e.g. "MANAGER" or "admin"
	 * @return true if there is a logged in user with the specified role name
	 * @see {@link YadaSecurityUtil#hasCurrentRole(String)}
	 */
	public boolean isCurrentRole(String roleString) {
		List<Integer> roles = getLoggedInUserRoles();
		if (roles!=null) {
			return roles.contains(config.getRoleId(roleString));
		}
		return false;
	}

	/**
	 * Check if the current user has the role "ADMIN"
	 * @return
	 */
	public boolean isAdmin() {
		return isCurrentRole("ADMIN");
	}

	/**
	 * Check if the argument userProfile is the same as the currently logged-in one
	 * @param someUserProfile
	 * @deprecated use {@link #isLoggedInUser(YadaUserProfile)} instead
	 */
	@Deprecated
	public boolean isLoggedUser(T someUserProfile) {
		return isLoggedInUser(someUserProfile);
	}
	
	/**
	 * Check if the argument userProfile is the same as the currently logged-in one
	 * @param someUserProfile
	 */
	public boolean isLoggedInUser(T someUserProfile) {
		return someUserProfile!=null &&
				someUserProfile.getId()!=null &&
				someUserProfile.getId().equals(getCurrentUserProfileId());
	}

	/**
	 * Check if the current user is authenticated (logged in) not anonymously.
	 */
	public boolean isLoggedIn() {
		return yadaSecurityUtil.isLoggedIn();
	}
	
	/**
	 * @return Returns the numeric roles that the currently logged in user has, or null when not logged in
	 */
	public List<Integer> getLoggedInUserRoles() {
		return isLoggedIn() ? getCurrentUserProfile().getUserCredentials().getRoles() : null;
	}
	
	/**
	 * @return Returns the sorted role names that the currently logged in user has, or null when not logged in
	 */
	public List<String> getLoggedInUserRoleKeys() {
		return isLoggedIn() ? getLoggedInUserRoles().stream().map(config::getRoleKey).sorted().collect(Collectors.toList()) : null;
	}

	/**
	 * Returns the id of the YadaUserProfile for the currently logged-in user, if any
	 * @return the id or null
	 */
	public Long getCurrentUserProfileId() {
		if (loggedInUserProfileId==null && yadaSecurityUtil!=null) {
			String username = yadaSecurityUtil.getUsername();
			if (username!=null) {
				loggedInUserProfileId = yadaUserProfileDao.findUserProfileIdByUsername(username);
			}
		}
		return loggedInUserProfileId;
	}

	/**
	 * Returns the currently logged-in user profile or null
	 * @return
	 */
	public T getCurrentUserProfile() {
		if (loggedInUserProfileId==null) {
			getCurrentUserProfileId();
		}
		Optional<T> result = loggedInUserProfileId==null?null:yadaUserProfileDao.findById(loggedInUserProfileId);
		return (result == null || !result.isPresent())?null:result.get();
	}

	/**
	 * Returns true if there are images to be cropped
	 */
	public boolean hasCropQueue() {
		return this.cropQueue != null && this.cropQueue.hasCropImages();
	}

	/**
	 * Returns the current YadaCropQueue
	 * @return the YadaCropQueue or null
	 */
	public YadaCropQueue getCropQueue() {
		return this.cropQueue;
	}

	public void deleteCropQueue() {
		if (this.cropQueue != null) {
			this.cropQueue.delete();
		}
		this.cropQueue = null;
	}

	/**
	 * Starts a new crop operation deleting any stale images.
	 * @param cropRedirect where to go to perform the crop, e.g. "/some/controller/cropPage"
	 * @param destinationRedirect where to go after all the crop has been done, e.g. "/some/controller/afterCrop"
	 * @return
	 */
	public YadaCropQueue addCropQueue(String cropRedirect, String destinationRedirect) {
		if (this.cropQueue != null) {
			this.cropQueue.delete();
		}
		this.cropQueue = new YadaCropQueue(cropRedirect, destinationRedirect);
		this.cropQueue = (YadaCropQueue) yadaUtil.autowireAndInitialize(this.cropQueue);
		return this.cropQueue;
	}

	@SuppressWarnings("unused")
	private void setCropQueue(YadaCropQueue cropQueue) {
		this.cropQueue = cropQueue;
	}

}
