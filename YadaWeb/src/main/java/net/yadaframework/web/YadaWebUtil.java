package net.yadaframework.web;

import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_BODY;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_REDIRECT;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_SEVERITY;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_TITLE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_AUTOCLOSE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_RELOADONCLOSE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_TOTALSEVERITY;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_CALLSCRIPT;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_ERROR;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_INFO;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_OK;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Entities.EscapeMode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.entity.YadaRegistrationRequest;
import net.yadaframework.persistence.repository.YadaRegistrationRequestRepository;

@Service
public class YadaWebUtil {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private final static int MAX_AGE_DAY=20; // Tempo dopo il quale una richiesta viene cancellata
	private final static long MILLIS_IN_DAY = 24*60*60*1000; // Millesimi di secondo in un giorno
	
	private final static String SAVED_REQUEST = "SPRING_SECURITY_SAVED_REQUEST"; // copiato da org.springframework.security.web.savedrequest.HttpSessionRequestCache
	
	@Autowired HttpSession httpSession; // Funziona perchè è un proxy
	@Autowired YadaConfiguration config;
	@Autowired YadaUtil yadaUtil;
	@Autowired YadaRegistrationRequestRepository registrationRequestRepository;
	
	private Date lastOldCleanup = null; // Data dell'ultimo cleanup, ne viene fatto uno al giorno
	private Object lastOldCleanupMonitor = new Object();
	
	public final Pageable FIND_ONE = new PageRequest(0, 1); 
	
	/**
	 * Encodes a string with URLEncoder, handling the useless try-catch that is needed
	 * @param source
	 * @return
	 */
	public String urlEncode(String source) {
		final String encoding = "UTF-8";
		try {
			return URLEncoder.encode(source, encoding);
		} catch (UnsupportedEncodingException e) {
			log.error("Invalid encoding: {}", encoding);
		}
		return source;
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
	 * ATTENZIONE: non sempre va!
	 * @return
	 */
	public HttpServletRequest getCurrentRequest() {
		return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
	}
	
	/**
	 * Ritorna true se la request corrente è ajax
	 * @return
	 */
	public boolean isAjaxRequest() {
		// 		if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
		String ajaxHeader = getCurrentRequest().getHeader("X-Requested-With");
		return "XMLHttpRequest".equals(ajaxHeader);
	}
	
	/**
	 * Trasforma /res/img/favicon.ico in /res-0002/img/favicon.ico
	 * @param urlNotVersioned
	 * @return
	 */
	public String versionifyResourceUrl(String urlNotVersioned) {
		// Esempio: var urlPrefix=[[@{${@yadaWebUtil.versionifyResourceUrl('/res/js/facebook/sdk.js')}}]];

		final String resFolder="/" + config.getResourceDir() + "/"; // "/res/"
		final int resLen = resFolder.length();
		if (urlNotVersioned.startsWith(resFolder)) {
			try {
				String prefix = urlNotVersioned.substring(0, resLen-1);
				String suffix = urlNotVersioned.substring(resLen-1);
				return prefix + "-" + config.getApplicationBuild() + suffix;
			} catch (Exception e) {
				log.error("Impossibile versionificare la url {} (ignored)", urlNotVersioned, e);
			} 
		}
    	return urlNotVersioned;
    }
	
	/**
	 * Pulisce l'html lasciando solo i seguenti tag: b, em, i, strong, u, "br", "cite", "em", "i", "p", "strong", "img", "li", "ul", "ol", "sup", "sub", "s"
	 * @param content
	 * @return
	 */
	public String cleanContent(String content) {
		Whitelist allowedTags = Whitelist.simpleText(); // This whitelist allows only simple text formatting: b, em, i, strong, u. All other HTML (tags and attributes) will be removed.
		allowedTags.addTags("br", "cite", "em", "i", "p", "strong", "img", "li", "ul", "ol", "sup", "sub", "s");
		allowedTags.addAttributes("p", "style"); // Serve per l'allineamento a destra e sinistra
		allowedTags.addAttributes("img", "src", "style", "class"); 
		Document dirty = Jsoup.parseBodyFragment(content, "");
		Cleaner cleaner = new Cleaner(allowedTags);
		Document clean = cleaner.clean(dirty);
		clean.outputSettings().escapeMode(EscapeMode.xhtml); // Non fa l'escape dei caratteri utf-8
		String safe = clean.body().html();
		return safe;
	}

	/**
	 * Cancello le registration request vecchie o con lo stesso email e tipo. Se la registrationRequest passata è sul database, non viene cancellata.
	 * @param registrationRequest prototipo di richiesta da cancellare (ne viene usato email e tipo)
	 */
	public void registrationRequestCleanup(YadaRegistrationRequest registrationRequest) {
		Date now = new Date();
		// Cancello registrazioni vecchie. Devo sincronizzare per evitare che la delete fallisca in caso di sovrapposizione di più utenti.
		// TODO non potevo fare tutto con una semplice query?!!!
		synchronized (lastOldCleanupMonitor) {
			if (lastOldCleanup==null || now.getTime()-lastOldCleanup.getTime() > MILLIS_IN_DAY) { // Faccio pulizia ogni 24 ore
				lastOldCleanup = now;
				Date limit = new Date(now.getTime() - MAX_AGE_DAY * MILLIS_IN_DAY); // Pulisco le righe più vecchie di MAX_AGE_DAY giorni
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
	 * Ritorna l'indirizzo completo della webapp, tipo http://www.yodadog.net:8080/site, senza slash finale
	 * Da thymeleaf si usa con ${@yadaWebUtil.getWebappAddress(#httpServletRequest)}
	 * @param request
	 * @return
	 */
	public String getWebappAddress(HttpServletRequest request) {
		int port = request.getServerPort();
		String pattern = port==80?"%s://%s%s":"%s://%s:%d%s";
		String myServerAddress = port==80? 
				String.format(pattern, request.getScheme(),  request.getServerName(), request.getContextPath())
				:
				String.format(pattern, request.getScheme(),  request.getServerName(), request.getServerPort(), request.getContextPath());
		return myServerAddress;
	}

	/**
	 * Indica se l'estensione appartiene a un'immagine accettabile
	 * @param extension e.g. "jpg"
	 * @return
	 */
	public boolean isWebImage(String extension) {
		extension = extension.toLowerCase();
		return extension.equals("jpg") 
			|| extension.equals("png") 
			|| extension.equals("gif") 
			|| extension.equals("tif") 
			|| extension.equals("tiff") 
			|| extension.equals("jpeg");
	}
	
	/**
	 * ritorna ad esempio "jpg" lowercase
	 * @param multipartFile
	 * @return
	 */
	public String getFileExtension(MultipartFile multipartFile) {
		String result = null;
		if (multipartFile!=null) {
			String originalName = multipartFile.getOriginalFilename();
			result = yadaUtil.getFileExtension(originalName);
		}
		return result;
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
	 * Visualizza un errore se non viene fatto redirect
	 * @param title
	 * @param message
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public String modalError(String title, String message, Model model) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_ERROR, null, model);
		return "/yada/modalNotify";
	}
	
	/**
	 * 
	 * @param title
	 * @param message
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public String modalInfo(String title, String message, Model model) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_INFO, null, model);
		return "/yada/modalNotify";
	}
	
	/**
	 * 
	 * @param title
	 * @param message
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public String modalOk(String title, String message, Model model) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_OK, null, model);
		return "/yada/modalNotify";
	}
	
	/**
	 * Visualizza un errore se viene fatto redirect
	 * @param title
	 * @param message
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void modalError(String title, String message, RedirectAttributes redirectAttributes) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_ERROR, redirectAttributes);
	}
	
	/**
	 * 
	 * @param title
	 * @param message
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void modalInfo(String title, String message, RedirectAttributes redirectAttributes) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_INFO, redirectAttributes);
	}
	
	/**
	 * 
	 * @param title
	 * @param message
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void modalOk(String title, String message, RedirectAttributes redirectAttributes) {
		notifyModal(title, message, VAL_NOTIFICATION_SEVERITY_OK, redirectAttributes);
	}
	
	/**
	 * Set the automatic closing time for the notification - no close button is shown
	 * @param milliseconds
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void modalAutoclose(long milliseconds, Model model) {
		model.addAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
	}
	
	/**
	 * Set the automatic closing time for the notification - no close button is shown
	 * @param milliseconds
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void modalAutoclose(long milliseconds, RedirectAttributes redirectAttributes) {
		redirectAttributes.addAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
	}

	/**
	 * Set the page to reload when the modal is closed
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void modalReloadOnClose(Model model) {
		model.addAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
	}
	
	/**
	 * Set the page to reload when the modal is closed
	 * @param redirectAttributes
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void modalReloadOnClose(RedirectAttributes redirectAttributes) {
		redirectAttributes.addAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
	}
	
	@Deprecated 
	private void notifyModal(String title, String message, String severity, RedirectAttributes redirectAttributes) {
		Map<String, ?> modelMap = redirectAttributes.getFlashAttributes();
		// Mette nel flash tre array di stringhe che contengono titolo, messaggio e severity.
		if (!modelMap.containsKey(KEY_NOTIFICATION_TITLE)) {
			List<String> titles = new ArrayList<String>();
			List<String> bodies = new ArrayList<String>();
			List<String> severities = new ArrayList<String>();
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_TITLE, titles);
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_BODY, bodies);
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_SEVERITY, severities);
		}
		// Aggiunge i nuovi valori
		((List<String>)modelMap.get(KEY_NOTIFICATION_TITLE)).add(title);
		((List<String>)modelMap.get(KEY_NOTIFICATION_BODY)).add(message);
		((List<String>)modelMap.get(KEY_NOTIFICATION_SEVERITY)).add(severity);
		String newTotalSeverity = calcTotalSeverity(modelMap, severity);
		redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_TOTALSEVERITY, newTotalSeverity);
	}
	
	/**
	 * Test if a modal is going to be opened when back to the view (usually after a redirect)
	 * @param request
	 * @return true if a modal is going to be opened
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public boolean isNotifyModalPending(HttpServletRequest request) {
		Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
		return flashMap!=null && (
			flashMap.containsKey(KEY_NOTIFICATION_TITLE)
			|| flashMap.containsKey(KEY_NOTIFICATION_BODY)
			|| flashMap.containsKey(KEY_NOTIFICATION_SEVERITY)
			);
	}
	
	/**
	 * Da usare direttamente solo quando si vuole fare un redirect dopo aver mostrato un messaggio.
	 * Se chiamato tante volte, i messaggi si sommano e vengono mostrati tutti all'utente.
	 * @param title
	 * @param message
	 * @param severity a string like YadaConstants.VAL_NOTIFICATION_SEVERITY_OK
	 * @param redirectSemiurl e.g. "/user/profile"
	 * @param model
	 * @see YadaConstants
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void notifyModal(String title, String message, String severity, String redirectSemiurl, Model model) {
		if (severity==VAL_NOTIFICATION_SEVERITY_ERROR) {
			// Tutte le notifiche di errore vengono loggate a warn (potrebbero non essere degli errori del programma)
			log.warn("notifyModal: {} - {}", title, message);
		}
		// Mette nel model tre array di stringhe che contengono titolo, messaggio e severity.
		if (!model.containsAttribute(KEY_NOTIFICATION_TITLE)) {
			List<String> titles = new ArrayList<String>();
			List<String> bodies = new ArrayList<String>();
			List<String> severities = new ArrayList<String>();
			model.addAttribute(KEY_NOTIFICATION_TITLE, titles);
			model.addAttribute(KEY_NOTIFICATION_BODY, bodies);
			model.addAttribute(KEY_NOTIFICATION_SEVERITY, severities);
		}
		// Aggiunge i nuovi valori
		Map<String, Object> modelMap = model.asMap();
		((List<String>)modelMap.get(KEY_NOTIFICATION_TITLE)).add(title);
		((List<String>)modelMap.get(KEY_NOTIFICATION_BODY)).add(message);
		((List<String>)modelMap.get(KEY_NOTIFICATION_SEVERITY)).add(severity);
		// Il redirect è sempre uno solo: prevale l'ultimo
		if (redirectSemiurl!=null) {
			model.addAttribute(KEY_NOTIFICATION_REDIRECT, redirectSemiurl);
		}
		String newTotalSeverity = calcTotalSeverity(modelMap, severity);
		model.addAttribute(KEY_NOTIFICATION_TOTALSEVERITY, newTotalSeverity);
	}
	
	/**
	 * Return true if modalError has been called before
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public boolean isModalError(Model model) {
		return model.asMap().containsValue(VAL_NOTIFICATION_SEVERITY_ERROR);
	}
	
	/**
	 * Return true if for this thread the notifyModal (or a variant) has been called
	 * @param model
	 * @return
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public boolean isNotifyModalRequested(Model model) {
		return model.containsAttribute(KEY_NOTIFICATION_TITLE);
	}

	/**
	 * Calcola la severity totale del dialog dalla severity di tutti i messaggi esistenti (è la più alta tra tutti)
	 * @param modelMap
	 * @param lastSeverity
	 * @return
	 */
	@Deprecated
	private String calcTotalSeverity(Map<String, ?> modelMap, String lastSeverity) {
		// Algoritmo:
		// - se la total è error, resta error;
		// - se lastSeverity è ok e quella total è info, resta info;
		// - per cui
		// - se lastSeverity è error, diventa o resta error;
		// - se lastSeverity è info, diventa o resta info;
		// - se lastSeverity è ok, diventa ok visto che total non è nè error nè info;
		String newTotalSeverity = lastSeverity;
		String currentTotalSeverity = (String) modelMap.get(KEY_NOTIFICATION_TOTALSEVERITY);
		if (VAL_NOTIFICATION_SEVERITY_ERROR.equals(currentTotalSeverity)) {
			return currentTotalSeverity; // ERROR wins always
		}
		if (VAL_NOTIFICATION_SEVERITY_OK.equals(lastSeverity) && VAL_NOTIFICATION_SEVERITY_INFO.equals(currentTotalSeverity)) {
			newTotalSeverity = currentTotalSeverity; // INFO wins over OK
		}
		return newTotalSeverity;
	}
	
//	private void notifyModalSetValue(String title, String message, String severity, String redirectSemiurl, Model model) {
//		model.addAttribute(KEY_NOTIFICATION_TITLE, title);
//		model.addAttribute(KEY_NOTIFICATION_BODY, message);
//		model.addAttribute(KEY_NOTIFICATION_SEVERITY, severity);
//		if (redirectSemiurl!=null) {
//			model.addAttribute(KEY_NOTIFICATION_REDIRECT, redirectSemiurl);
//		}
//	}

	public String getClientIp(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		String forwardedFor = request.getHeader("X-Forwarded-For");
		String remoteIp = "?";
		if (!StringUtils.isBlank(remoteAddr)) {
			remoteIp = remoteAddr;
		}
		if (!StringUtils.isBlank(forwardedFor)) {
			remoteIp = "[for " + forwardedFor + "]";
		}
		return remoteIp;
	}

	/**
	 * 
	 * @param message text to be displayed (can be null for default value)
	 * @param confirmButton text of the confirm button (can be null for default value)
	 * @param cancelButton text of the cancel button (can be null for default value)
	 * @param model
	 */
	public String modalConfirm(String message, String confirmButton, String cancelButton, Model model) {
		return modalConfirm(message, confirmButton, cancelButton, model, false, false);
	}
	
	/**
	 * Show the confirm modal and reloads when the confirm button is pressed, adding the confirmation parameter to the url.
	 * The modal will be opened on load.
	 * Usually used by non-ajax methods.
	 * @param message text to be displayed (can be null for default value)
	 * @param confirmButton text of the confirm button (can be null for default value)
	 * @param cancelButton text of the cancel button (can be null for default value)
	 * @param model
	 */
	public String modalConfirmAndReload(String message, String confirmButton, String cancelButton, Model model) {
		return modalConfirm(message, confirmButton, cancelButton, model, true, true);
	}
	
	/**
	 * Show the confirm modal and optionally reloads when the confirm button is pressed
	 * @param message text to be displayed (can be null for default value)
	 * @param confirmButton text of the confirm button (can be null for default value)
	 * @param cancelButton text of the cancel button (can be null for default value)
	 * @param model
	 * @param reloadOnConfirm (optional) when true, the browser will reload on confirm, adding the confirmation parameter to the url
	 * @param openModal (optional) when true, the modal will be opened. To be used when the call is not ajax
	 */
	public String modalConfirm(String message, String confirmButton, String cancelButton, Model model, Boolean reloadOnConfirm, Boolean openModal) {
		model.addAttribute("message", message);
		model.addAttribute("confirmButton", confirmButton);
		model.addAttribute("cancelButton", cancelButton);
		if (openModal) {
			model.addAttribute("openModal", true);
		}
		if (reloadOnConfirm) {
			model.addAttribute("reloadOnConfirm", true);
		}
		return "/yada/modalConfirm";
	}

	/**
	 * Add a script id to call when opening the notification modal
	 * @param scriptId
	 * @param model
	 * @deprecated Use YadaNotify instead
	 */
	@Deprecated 
	public void callScriptOnModal(String scriptId, Model model) {
		if (!model.containsAttribute(KEY_NOTIFICATION_CALLSCRIPT)) {
			List<String> scriptIds = new ArrayList<String>();
			model.addAttribute(KEY_NOTIFICATION_CALLSCRIPT, scriptIds);
		}
		Map<String, Object> modelMap = model.asMap();
		((List<String>)modelMap.get(KEY_NOTIFICATION_CALLSCRIPT)).add(scriptId);
	}
	
}
