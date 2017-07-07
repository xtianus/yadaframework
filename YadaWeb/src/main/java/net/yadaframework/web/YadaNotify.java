package net.yadaframework.web;
import static net.yadaframework.core.YadaConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import net.yadaframework.exceptions.YadaInvalidUsageException;

/**
 * Build a notification dialog to be shown on the response page or after a redirect.
 * More than one notification can be saved for the same request: all messages will be shown in one dialog.
 * A single instance can be reused after changing its parameters. Multiple instances can be used too. 
 * The last method called on the instance must be yadaSave() to store the configuration values.
 * Example: <br/>
 * <pre>
return YadaNotify.instance(model).yadaOk()
	.yadaTitle("Info Accepted").yadaMessage("Thank you for your support")
	.yadaAutoclose(2000).yadaReloadOnClose().yadaSave();
</pre>
 */
@Component
// This is a factory of YadaNotifyData
public class YadaNotify {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	@Autowired private MessageSource messageSource;
	
	@Deprecated private Model model; 
	@Deprecated private RedirectAttributes redirectAttributes;
	@Deprecated private String severity;
	@Deprecated private String title;
	@Deprecated private String message;
	@Deprecated private Locale locale;
		
//	private YadaNotify() {
//	}
	
	/**
	 * Initialise an empty instance
	 * @param model
	 * @return
	 */
	public YadaNotifyData empty(Model model) {
		return new YadaNotifyData(model, messageSource, null);
	}
	
	/**
	 * Initialise an empty instance
	 * @param model
	 * @return
	 */
	public YadaNotifyData empty(RedirectAttributes redirectAttributes) {
		return new YadaNotifyData(redirectAttributes, messageSource, null);
	}

	/**
	 * Initialise the instance
	 * @param title the notification title
	 * @param model
	 * @return
	 */
	public YadaNotifyData title(String title, Model model) {
		return new YadaNotifyData(model, messageSource, null).setTitle(title);
	}
	
	/**
	 * Initialise the instance
	 * @param title the notification title
	 * @param model
	 * @return
	 */
	public YadaNotifyData title(String title, Model model, Locale locale) {
		return new YadaNotifyData(model, messageSource, locale).setTitle(title);
	}

	
	/**
	 * Initialise the instance
	 * @param title the notification title
	 * @param redirectAttributes
	 * @return
	 */
	public YadaNotifyData title(String title, RedirectAttributes redirectAttributes) {
		return new YadaNotifyData(redirectAttributes, messageSource, null).setTitle(title);
	}

	/**
	 * Initialise the instance
	 * @param title the notification title
	 * @param redirectAttributes
	 * @return
	 */
	public YadaNotifyData title(String title, RedirectAttributes redirectAttributes, Locale locale) {
		return new YadaNotifyData(redirectAttributes, messageSource, locale).setTitle(title);
	}

	/**
	 * Initialise the instance
	 * @param model
	 * @param titleKeyAndArgs the title key, followed by optional arguments to be replaced in the localized value
	 * @return
	 */
	public YadaNotifyData titleKey(Model model, String ... titleKeyAndArgs) {
		return new YadaNotifyData(model, messageSource, null).setTitleKey(titleKeyAndArgs);
	}

	/**
	 * Initialise the instance
	 * @param model
	 * @param titleKeyAndArgs the title key, followed by optional arguments to be replaced in the localized value
	 * @return
	 */
	public YadaNotifyData titleKey(Model model, Locale locale, String ... titleKeyAndArgs) {
		return new YadaNotifyData(model, messageSource, locale).setTitleKey(titleKeyAndArgs);
	}
	
	/**
	 * Initialise the instance
	 * @param model
	 * @param titleKeyAndArgs the title key, followed by optional arguments to be replaced in the localized value
	 * @return
	 */
	public YadaNotifyData titleKey(RedirectAttributes redirectAttributes, String ... titleKeyAndArgs) {
		return new YadaNotifyData(redirectAttributes, messageSource, null).setTitleKey(titleKeyAndArgs);
	}
	
	/**
	 * Initialise the instance
	 * @param model
	 * @param titleKeyAndArgs the title key, followed by optional arguments to be replaced in the localized value
	 * @return
	 */
	public YadaNotifyData titleKey(RedirectAttributes redirectAttributes, Locale locale, String ... titleKeyAndArgs) {
		return new YadaNotifyData(redirectAttributes, messageSource, locale).setTitleKey(titleKeyAndArgs);
	}
	
	
	/**
	 * Test if a modal is going to be opened when back to the view (usually after a redirect)
	 * @param request
	 * @return true if a modal is going to be opened
	 */
	public boolean isNotificationPending(HttpServletRequest request) {
		Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
		return flashMap!=null && (
			flashMap.containsKey(KEY_NOTIFICATION_TITLE)
			|| flashMap.containsKey(KEY_NOTIFICATION_BODY)
			|| flashMap.containsKey(KEY_NOTIFICATION_SEVERITY)
			);
	}
	
	/**
	 * Test if a modal is going to be opened when back to the view
	 * @param model can be null
	 * @param redirectAttributes can be null if model is not null
	 * @return
	 */
	public boolean isNotificationPending(Model model, RedirectAttributes redirectAttributes) {
		boolean result = false;
		if (model!=null) {
			result = model.containsAttribute(KEY_NOTIFICATION_TITLE);
		}
		if (redirectAttributes!=null) {
			result |= redirectAttributes.getFlashAttributes().containsKey(KEY_NOTIFICATION_TITLE);
		}
		return result;
	}

	/**
	 * Return true if error() has been called before
	 * @param model can be null
	 * @param redirectAttributes can be null
	 * @return
	 */
	public boolean isErrorSet(Model model, RedirectAttributes redirectAttributes) {
		boolean result = false;
		if (model!=null) {
			result = model.asMap().containsValue(VAL_NOTIFICATION_SEVERITY_ERROR);
		}
		if (redirectAttributes!=null) {
			result |= redirectAttributes.getFlashAttributes().containsValue(VAL_NOTIFICATION_SEVERITY_ERROR);
		}
		return result;
	}
	

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Create a new YadaNotify instance.
	 * @param model
	 * @return
	 * @deprecated use "@Autowired YadaNotify yadaNotify", followed by "yadaNotify.init(model)..."
	 */
	@Deprecated
	public static YadaNotify instance(Model model) {
		YadaNotify instance = new YadaNotify();
		instance.model = model;
		return instance;
	}

	/**
	 * Create a new YadaNotify instance to be shown after a redirect
	 * @param redirectAttributes
	 * @return
	 * @deprecated use "@Autowired YadaNotify yadaNotify", followed by "yadaNotify.init(redirectAttributes)..."
	 */
	@Deprecated
	public static YadaNotify instance(RedirectAttributes redirectAttributes) {
		YadaNotify instance = new YadaNotify();
		instance.redirectAttributes = redirectAttributes;
		return instance;
	}
	
	/**
	 * Makes the notification active. Can be called many times to add different notifications, even on the same instance.
	 * @return If used with a Model, returns the view of the notification modal.
	 */
	@Deprecated
	public String yadaSave() {
		if (redirectAttributes!=null) {
			activateRedirect();
			return null;
		}
		activateNormal();
		return "/yada/modalNotify";
	}

	/**
	 * Set the notification title
	 * @param title
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaTitle(String title) {
		this.title = title;
		return this;
	}
		
	/**
	 * Set the notification message. Can be HTML.
	 * @param title
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaMessage(String message) {
		this.message = message;
		return this;
	}
	
	/**
	 * Set the autoclose time in milliseconds - no close button is rendered
	 * @param milliseconds
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaAutoclose(long milliseconds) {
		if (model!=null) {
			model.addAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
		} else {
			redirectAttributes.addAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
		}
		return this;
	}

	/**
	 * Set the page to reload when the modal is closed
	 */
	@Deprecated
	public YadaNotify yadaReloadOnClose() {
		if (model!=null) {
			model.addAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
		} else {
			redirectAttributes.addAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
		}
		return this;
	}

	/**
	 * Set localization parameters
	 * @param messageSource
	 * @param locale
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaMessageSource(MessageSource messageSource, Locale locale) {
		this.messageSource = messageSource;
		this.locale = locale;
		return this;
	}

	/**
	 * Set the notification title using localization
	 * @param titleKeyAndArgs the title key, followed by optional arguments to be replaced in the localized value
	 * @throws YadaInvalidUsageException if yadaMessageSource() hasn't been called
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaTitleKey(String ... titleKeyAndArgs) {
		ensureLocalized();
		String[] argsArray = Arrays.copyOfRange(titleKeyAndArgs, 1, titleKeyAndArgs.length);
		this.title = messageSource.getMessage(titleKeyAndArgs[0], argsArray, locale);
		return this;
	}

	/**
	 * Set the notification message using localization. Can be HTML.
	 * @param messageKeyAndArgs the message key, followed by optional arguments to be replaced in the localized value
	 * @throws YadaInvalidUsageException if yadaMessageSource() hasn't been called
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaMessageKey(String ... messageKeyAndArgs) {
		ensureLocalized();
		String[] argsArray = Arrays.copyOfRange(messageKeyAndArgs, 1, messageKeyAndArgs.length);
		this.message = messageSource.getMessage(messageKeyAndArgs[0], argsArray, locale);
		return this;
	}
	
	/**
	 * The page will redirect on modal close
	 * @param path the last part of the url after the servlet context, like "/user/profile"
	 * @return
	 */
	@Deprecated
	public YadaNotify redirectOnClose(String path) {
		if (model!=null) {
			model.addAttribute(KEY_NOTIFICATION_REDIRECT, path);
		} else {
			redirectAttributes.addAttribute(KEY_NOTIFICATION_REDIRECT, path);
		}
		return this;
	}


	/**
	 * Add a script id to call when opening the notification modal. The script must be inserted into "/script.html"
	 * @param scriptId
	 */
	@Deprecated
	public YadaNotify script(String scriptId) {
		if (model!=null) {
			if (!model.containsAttribute(KEY_NOTIFICATION_CALLSCRIPT)) {
				List<String> scriptIds = new ArrayList<String>();
				model.addAttribute(KEY_NOTIFICATION_CALLSCRIPT, scriptIds);
			}
			Map<String, Object> modelMap = model.asMap();
			((List<String>)modelMap.get(KEY_NOTIFICATION_CALLSCRIPT)).add(scriptId);
		}
		if (redirectAttributes!=null) {
			Map<String, ?> modelMap = redirectAttributes.getFlashAttributes();
			if (!modelMap.containsKey(KEY_NOTIFICATION_CALLSCRIPT)) {
				List<String> scriptIds = new ArrayList<String>();
				redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_CALLSCRIPT, scriptIds);
			}
			((List<String>)modelMap.get(KEY_NOTIFICATION_CALLSCRIPT)).add(scriptId);		}
		return this;
	}

	/**
	 * Set the notification severity
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaOk() {
		this.severity = VAL_NOTIFICATION_SEVERITY_OK;
		return this;
	}
	
	/**
	 * Set the notification severity
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaInfo() {
		this.severity = VAL_NOTIFICATION_SEVERITY_INFO;
		return this;
	}
	
	/**
	 * Set the notification severity
	 * @return
	 */
	@Deprecated
	public YadaNotify yadaError() {
		this.severity = VAL_NOTIFICATION_SEVERITY_ERROR;
		return this;
	}
	
	@Deprecated
	private boolean isLocalized() {
		return messageSource!=null && locale !=null;
	}
	
	@Deprecated
	private void ensureLocalized() {
		if (!isLocalized()) {
			throw new YadaInvalidUsageException("The method yadaMessageSource() must be called before using keys");
		}
	}
	
	@Deprecated
	private void activateRedirect() {
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

	@Deprecated
	private void activateNormal() {
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
		String newTotalSeverity = calcTotalSeverity(modelMap, severity);
		model.addAttribute(KEY_NOTIFICATION_TOTALSEVERITY, newTotalSeverity);
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
	
	/**
	 * Test if a modal is going to be opened when back to the view (usually after a redirect)
	 * @param request
	 * @return true if a modal is going to be opened
	 */
	@Deprecated
	public static boolean isNotifyModalPending(HttpServletRequest request) {
		Map<String, ?> flashMap = RequestContextUtils.getInputFlashMap(request);
		return flashMap!=null && (
			flashMap.containsKey(KEY_NOTIFICATION_TITLE)
			|| flashMap.containsKey(KEY_NOTIFICATION_BODY)
			|| flashMap.containsKey(KEY_NOTIFICATION_SEVERITY)
			);
	}

	/**
	 * Return true if yadaError() has been called before
	 * @param model can be null
	 * @param redirectAttributes can be null
	 * @return
	 */
	@Deprecated
	public static boolean isYadaError(Model model, RedirectAttributes redirectAttributes) {
		boolean result = false;
		if (model!=null) {
			result = model.asMap().containsValue(VAL_NOTIFICATION_SEVERITY_ERROR);
		}
		if (redirectAttributes!=null) {
			result |= redirectAttributes.getFlashAttributes().containsValue(VAL_NOTIFICATION_SEVERITY_ERROR);
		}
		return result;
	}
	
	
	/**
	 * Check if the YadaNotify.yadaSave() has been called in this request
	 * @param model can be null
	 * @param redirectAttributes can be null
	 * @return
	 */
	@Deprecated
	public static boolean isYadaNotifySaved(Model model, RedirectAttributes redirectAttributes) {
		boolean result = false;
		if (model!=null) {
			result = model.containsAttribute(KEY_NOTIFICATION_TITLE);
		}
		if (redirectAttributes!=null) {
			result |= redirectAttributes.getFlashAttributes().containsKey(KEY_NOTIFICATION_TITLE);
		}
		return result;
	}

}
