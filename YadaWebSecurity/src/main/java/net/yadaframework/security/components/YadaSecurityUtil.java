package net.yadaframework.security.components;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.components.YadaWebUtil;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaRegistrationRequestDao;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.web.form.YadaFormPasswordChange;

@Component
public class YadaSecurityUtil {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());

	private Date lastOldCleanup = null; // Data dell'ultimo cleanup, ne viene fatto uno al giorno
	private Object lastOldCleanupMonitor = new Object();

	private final static int MAX_AGE_DAY=20; // Tempo dopo il quale una richiesta viene cancellata
	private final static long MILLIS_IN_DAY = 24*60*60*1000; // Millesimi di secondo in un giorno
	private final static String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST"; // copiato da org.springframework.security.web.savedrequest.HttpSessionRequestCache

	private SecureRandom secureRandom = new SecureRandom();

	@Autowired private HttpSession httpSession; // Funziona perchè è un proxy
	@Autowired private YadaTokenHandler yadaTokenHandler;
	@Autowired private YadaRegistrationRequestDao yadaRegistrationRequestDao;
	@Autowired private YadaUserDetailsService yadaUserDetailsService;
	@Autowired private YadaUserCredentialsDao yadaUserCredentialsDao;
	@Autowired private YadaWebUtil yadaWebUtil;


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
	 * @return
	 */
	public boolean performPasswordChange(YadaFormPasswordChange yadaFormPasswordChange) {
		long[] parts = yadaTokenHandler.parseLink(yadaFormPasswordChange.getToken());
		try {
			if (parts!=null && parts.length==2) {
				YadaRegistrationRequest registrationRequest = yadaRegistrationRequestDao.findByIdAndTokenOrderByTimestampDesc(parts[0], parts[1]).get(0);
				if (registrationRequest==null) {
					return false;
				}
				String username = registrationRequest.getEmail();
				YadaUserCredentials yadaUserCredentials = yadaUserCredentialsDao.findFirstByUsername(StringUtils.trimToEmpty(username).toLowerCase());
				if (yadaUserCredentials!=null) {
					yadaUserCredentials = yadaUserCredentialsDao.changePassword(yadaUserCredentials, yadaFormPasswordChange.getPassword());
					yadaRegistrationRequestDao.delete(registrationRequest);
					if (yadaUserCredentials.isEnabled()) {
						yadaUserDetailsService.authenticateAs(yadaUserCredentials);
					}
					log.info("PASSWORD CHANGE for user='{}'", username);
					return true;
				}
			}
		} catch (Exception e) {
			log.info("Password change failed", e);
		}
		return false;
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
	 * Ritorna uno o l'altro parametro a seconda che l'utente corrente sia autenticato o meno
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
			List<YadaRegistrationRequest> ownRequests = yadaRegistrationRequestDao.findByEmailAndRegistrationType(registrationRequest.getEmail(), registrationRequest.getRegistrationType());
			for (YadaRegistrationRequest deletable : ownRequests) {
				if (deletable.getId()!=registrationRequest.getId()) {
					yadaRegistrationRequestDao.delete(deletable);
					log.info("Previous RegistrationRequest ({}) deleted", deletable);
				}
			}
		}
	}

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
	 * Controlla se l'utente attuale possiede il ruolo specificato. Case Sensitive!
	 * @param roleToCheck nel formato senza ROLE_ iniziale
	 * @return
	 */
	public boolean hasCurrentRole(String roleToCheck) {
		Set<String> roles = getCurrentRoles();
		return roles.contains(roleToCheck);
	}

	/**
	 * Controlla se l'utente attuale possiede almeno un ruolo tra quelli specificati. Case Sensitive!
	 * @param roleToCheck array di ruoli nel formato senza ROLE_ iniziale
	 * @return
	 */
	public boolean hasCurrentRole(String[] rolesToCheck) {
		Set<String> currentRoles = getCurrentRoles();
		Set<String> requiredRoles = new HashSet<>(Arrays.asList(rolesToCheck));
		return CollectionUtils.containsAny(currentRoles, requiredRoles);
	}
}
