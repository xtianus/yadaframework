package net.yadaframework.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.repository.YadaFileManagerDao;
import net.yadaframework.security.YadaUserDetailsService;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsRepository;
import net.yadaframework.security.persistence.repository.YadaUserProfileRepository;

/**
 * Base class for application session. The subclass must be annotated with "@Primary" otherwise two different instances are created for the two classes
 */
@Component
@SessionScope
// NOTE: Use @Primary in subclasses
public class YadaSession<T extends YadaUserProfile> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	@Autowired private YadaConfiguration config;
	@Autowired protected YadaSecurityUtil yadaSecurityUtil;
	// Attenzione: questo sarebbe da mettere transient perchè tomcat tenta di persistere la session ma non ce la fa. Pero' se lo si mette transient,
	// quando la session viene ricaricata questo non viene valorizzato. Come si fa a inizializzare questo oggetto quando tomcat lo ricarica dallo storage?
	@Autowired protected YadaUserProfileRepository<T> yadaUserProfileRepository;
	@Autowired protected YadaUserDetailsService yadaUserDetailsService;
	@Autowired protected YadaUserCredentialsRepository yadaUserCredentialsRepository;
	@Autowired protected YadaFileManagerDao yadaFileManagerDao;
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
		YadaUserCredentials targetUserCredentials = yadaUserCredentialsRepository.findByUserProfileId(targetUserProfileId);
		yadaUserDetailsService.authenticateAs(targetUserCredentials, false);
		loggedInUserProfileId = targetUserProfileId;
		log.info("Impersonification by #{} as {} started", impersonatorUserId, targetUserCredentials);
	}

	/**
	 * Use depersonate() instead.
	 * @return
	 */
	@Deprecated // Use depersonate()
	public boolean depersonify() {
		return depersonate();
	}

	/**
	 * Terminates impersonation.
	 * @return true if the impersonation was active, false if it was not active.
	 */
	public boolean depersonate() {
		if (isImpersonationActive()) {
			YadaUserCredentials originalCredentials = yadaUserCredentialsRepository.findByUserProfileId(impersonatorUserId);
			yadaUserDetailsService.authenticateAs(originalCredentials);
			log.info("Impersonification by {} ended", originalCredentials);
			clearCaches();
			return true;
		} else {
			log.error("Depersonification failed because of null impersonificator or null original user");
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
			return yadaUserProfileRepository.findRoleIds(idToCheck).contains(config.getRoleId("ADMIN"));
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
	 * Returns the id of the YadaUserProfile for the currently logged-in user
	 * @return the id or null
	 */
	public Long getCurrentUserProfileId() {
		if (loggedInUserProfileId==null && yadaSecurityUtil!=null) {
			String username = yadaSecurityUtil.getUsername();
			if (username!=null) {
				loggedInUserProfileId = yadaUserProfileRepository.findUserProfileIdByUsername(username);
			}
		}
		return loggedInUserProfileId;
	}

	/**
	 * Returns the currently logged-in user profile
	 * @return
	 */
	public T getCurrentUserProfile() {
		if (loggedInUserProfileId==null) {
			getCurrentUserProfileId();
		}
		return loggedInUserProfileId==null?null:yadaUserProfileRepository.findOne(loggedInUserProfileId);
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
