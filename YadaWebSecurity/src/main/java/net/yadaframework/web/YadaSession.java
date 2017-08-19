package net.yadaframework.web;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.security.YadaUserDetailsService;
import net.yadaframework.security.components.YadaSecurityUtil;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.entity.YadaUserProfile;
import net.yadaframework.security.persistence.repository.YadaUserProfileRepository;

/**
 *
 */
@Component
// ScopedProxyMode.TARGET_CLASS makes so that the instance is different for each thread, even if it is injected in a singleton
@Scope(value="session", proxyMode=ScopedProxyMode.TARGET_CLASS) 
public class YadaSession<T extends YadaUserProfile> {
	private final Logger log = LoggerFactory.getLogger(getClass());
	@Autowired private YadaConfiguration config;
	@Autowired private YadaWebUtil yadaWebUtil;
	@Autowired private YadaSecurityUtil yadaSecurityUtil;
	// Attenzione: questo sarebbe da mettere transient perchè tomcat tenta di persistere la session ma non ce la fa. Pero' se lo si mette transient,
	// quando la session viene ricaricata questo non viene valorizzato. Come si fa a inizializzare questo oggetto quando tomcat lo ricarica dallo storage?
	@Autowired private YadaUserProfileRepository<T> yadaUserProfileRepository;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;

	private T userProfile = null;
	private T impersonificatore = null;
	
	public void clearUserProfileCache() {
		this.userProfile = null;
	}
	
	public void clearCaches() {
		this.userProfile = null;
		this.impersonificatore = null;
	}
	
	public boolean isImpersonification() {
		return impersonificatore!=null;
	}
	
	public void impersonifica(T theUser) {
		impersonificatore = this.getCurrentUserProfile();
		userProfile = theUser;
		YadaUserCredentials yadaUserCredentials = theUser.getUserCredentials();
		yadaUserDetailsService.authenticateAs(yadaUserCredentials, false);
		log.info("Impersonificazione di {} come {} iniziata", impersonificatore, theUser);
	}
	
	public void depersonifica() {
		T theUser = userProfile;
		if (impersonificatore!=null) {
			userProfile = impersonificatore;
			impersonificatore = null;
			YadaUserCredentials yadaUserCredentials = userProfile.getUserCredentials();
			yadaUserDetailsService.authenticateAs(yadaUserCredentials);
			log.info("Impersonificazione di {} come {} terminata", userProfile, theUser);
		} else {
			log.error("Depersonificazione fallita perchè impersonificatore assente");
		}
	}

	public boolean isAdmin() {
		boolean result = false;
		if (getCurrentUserProfile()!=null) {
			YadaUserCredentials userCredentials = userProfile.getUserCredentials();
			if (userCredentials!=null) {
				List<Integer> roles = userCredentials.getRoles();
				if (roles!=null) {
					result = roles.contains(config.getRoleId("ADMIN"));
				}
			}
		}
		return result;
	}
	
	/**
	 * Ritorna true se lo UserProfile coincide con quello dell'utente loggato
	 * @param someUserProfile
	 * @return
	 */
	public boolean isLoggedUser(T someUserProfile) {
		T userProfile = getCurrentUserProfile();
		if (userProfile!=null && someUserProfile!=null) {
			return userProfile.getId().equals(someUserProfile.getId());
		}
		return false;
	}
	
	public T getCurrentUserProfile() {
		if (userProfile==null && yadaSecurityUtil!=null) {
			String username = yadaSecurityUtil.getUsername();
			if (username!=null) {
				List<T> userProfiles = yadaUserProfileRepository.findByUserCredentialsUsername(username, yadaWebUtil.FIND_ONE);
				if (userProfiles.size()==1) {
					userProfile = userProfiles.get(0);
				}
			}
		}
		return userProfile;
	}
	
}
