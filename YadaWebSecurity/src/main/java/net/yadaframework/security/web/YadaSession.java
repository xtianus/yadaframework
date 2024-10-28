package net.yadaframework.security.web;

import java.util.Optional;

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

	protected YadaCropQueue cropQueue; // Crop images

	public void clearUserProfileCache() {
		loggedInUserProfileId = null;
	}

	public void clearCaches() {
		impersonatorUserId = null;
		loggedInUserProfileId = null;
		impersonatedUserId = null;
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
		impersonatorUserId = getCurrentUserProfileId();
		impersonatedUserId = targetUserProfileId;
		YadaUserCredentials targetUserCredentials = yadaUserCredentialsDao.findByUserProfileId(targetUserProfileId);
		yadaUserDetailsService.authenticateAs(targetUserCredentials, false);
		loggedInUserProfileId = targetUserProfileId;
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
		return deimpersonate(null, null);
	}

	/**
	 * @deprecated Use {@link #deimpersonate(HttpServletRequest, HttpServletResponse) instead
	 */
	@Deprecated
	public boolean depersonate(HttpServletRequest request, HttpServletResponse response) {
		return deimpersonate(request, response);
	}
	
	/**
	 * Terminates impersonation.
	 * @return true if the impersonation was active, false if it was not active.
	 */
	public boolean deimpersonate(HttpServletRequest request, HttpServletResponse response) {
		if (isImpersonationActive()) {
			YadaUserCredentials originalCredentials = yadaUserCredentialsDao.findByUserProfileId(impersonatorUserId);
			yadaUserDetailsService.authenticateAs(originalCredentials, request, response);
			log.info("Impersonation by #{} ended", originalCredentials.getId());
			clearCaches();
			return true;
		} else {
			log.error("Deimpersonation failed because of null impersonator or null original user");
			return false;
		}
	}

	/**
	 * Check if the current user has the role "ADMIN"
	 * @return
	 */
	public boolean isAdmin() {
		Long idToCheck = getCurrentUserProfileId();
		if (idToCheck!=null) {
			return yadaUserProfileDao.findRoleIds(idToCheck).contains(config.getRoleId("ADMIN"));
		}
		return false;
	}

	/**
	 * Check if the argument userProfile is the same as the currently logged-in one
	 * @param someUserProfile
	 * @return
	 */
	public boolean isLoggedUser(T someUserProfile) {
		return someUserProfile!=null &&
			someUserProfile.getId()!=null &&
			someUserProfile.getId().equals(loggedInUserProfileId);
	}

	/**
	 * Check if the current user is authenticated (logged in) not anonymously.
	 */
	public boolean isLoggedIn() {
		return yadaSecurityUtil.isLoggedIn();
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
