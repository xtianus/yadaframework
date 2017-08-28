package net.yadaframework.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.YadaUserDetailsService;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsRepository;
import net.yadaframework.security.persistence.repository.YadaUserProfileRepository;

/**
 *
 */
@Component
@SessionScope
public class YadaSession<T extends YadaUserProfile> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	@Autowired private YadaConfiguration config;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	// Attenzione: questo sarebbe da mettere transient perchè tomcat tenta di persistere la session ma non ce la fa. Pero' se lo si mette transient,
	// quando la session viene ricaricata questo non viene valorizzato. Come si fa a inizializzare questo oggetto quando tomcat lo ricarica dallo storage?
	@Autowired private YadaUserProfileRepository<T> yadaUserProfileRepository;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private YadaUserCredentialsRepository yadaUserCredentialsRepository;

	protected Long impersonificatorUserId = null;
	protected Long impersonifiedUserId = null;
	protected Long loggedInUserProfileId = null; // id of the current logged in user, be it "real" or "impersonificated"
	
	public void clearUserProfileCache() {
		loggedInUserProfileId = null;
	}
	
	public void clearCaches() {
		impersonificatorUserId = null;
		loggedInUserProfileId = null;
		impersonifiedUserId = null;
	}
	
	public boolean isImpersonificationActive() {
		return impersonificatorUserId!=null && impersonifiedUserId!=null;
	}
	
	public void impersonify(Long targetUserProfileId) {
		impersonificatorUserId = getCurrentUserProfileId();
		impersonifiedUserId = targetUserProfileId;
		YadaUserCredentials targetUserCredentials = yadaUserCredentialsRepository.findByUserProfileId(targetUserProfileId);
		yadaUserDetailsService.authenticateAs(targetUserCredentials, false);
		loggedInUserProfileId = targetUserProfileId;
		log.info("Impersonification by #{} as {} started", impersonificatorUserId, targetUserCredentials);
	}
	
	/**
	 * Terminates impersonification.
	 * @return true if the impersonification was active, false if it was not active.
	 */
	public boolean depersonify() {
		if (isImpersonificationActive()) {
			YadaUserCredentials originalCredentials = yadaUserCredentialsRepository.findByUserProfileId(impersonificatorUserId);
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
	 * @return
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
	
	
}
