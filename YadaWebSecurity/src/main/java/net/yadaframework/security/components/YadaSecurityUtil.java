package net.yadaframework.security.components;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.security.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.security.persistence.repository.YadaRegistrationRequestRepository;

@Component
public class YadaSecurityUtil {
	private final transient Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired private YadaRegistrationRequestRepository registrationRequestRepository;
	
	private Date lastOldCleanup = null; // Data dell'ultimo cleanup, ne viene fatto uno al giorno
	private Object lastOldCleanupMonitor = new Object();

	private final static int MAX_AGE_DAY=20; // Tempo dopo il quale una richiesta viene cancellata
	private final static long MILLIS_IN_DAY = 24*60*60*1000; // Millesimi di secondo in un giorno
	private final static String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST"; // copiato da org.springframework.security.web.savedrequest.HttpSessionRequestCache
	
	@Autowired private HttpSession httpSession; // Funziona perchè è un proxy

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
	 * Use in thymeleaf with th:if="${@yadaWebUtil.loggedIn()}"
	 * @return
	 */
	@Deprecated // use isLoggedIn() instead
	public boolean loggedIn() {
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
	 * Check if the current user is authenticated (logged in) not anonymously.
	 * Use in thymeleaf with th:if="${@yadaWebUtil.loggedIn}"
	 * @return
	 */
	public boolean isLoggedIn() {
		return loggedIn();
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
				List<YadaRegistrationRequest> oldRequests = registrationRequestRepository.findByTimestampBefore(limit);
				if (oldRequests.isEmpty()) {
					log.info("No old RegistrationRequest to delete");
				} else {
					for (YadaRegistrationRequest deletable : oldRequests) {
						registrationRequestRepository.delete(deletable);
						log.info("Expired RegistrationRequest ({}) deleted", deletable);
					}
				}
			}
			// Cancello la precedente richiesta di registrazione per lo stesso email e stesso tipo
			List<YadaRegistrationRequest> ownRequests = registrationRequestRepository.findByEmailAndRegistrationType(registrationRequest.getEmail(), registrationRequest.getRegistrationType());
			for (YadaRegistrationRequest deletable : ownRequests) {
				if (deletable.getId()!=registrationRequest.getId()) {
					registrationRequestRepository.delete(deletable);
					log.info("Previous RegistrationRequest ({}) deleted", deletable);
				}
			}
		}
	}
	
	public Set<String> getCurrentRoles() {
		Set<String> roles = new HashSet<String>();
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
		Set<String> requiredRoles = new HashSet<String>(Arrays.asList(rolesToCheck));
		return CollectionUtils.containsAny(currentRoles, requiredRoles);
	}	
}