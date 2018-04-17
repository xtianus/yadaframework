package net.yadaframework.web;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_AUTOCLOSE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_BODY;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_CALLSCRIPT;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_REDIRECT;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_RELOADONCLOSE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_SEVERITY;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_TITLE;
import static net.yadaframework.core.YadaConstants.KEY_NOTIFICATION_TOTALSEVERITY;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_ERROR;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_INFO;
import static net.yadaframework.core.YadaConstants.VAL_NOTIFICATION_SEVERITY_OK;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import net.yadaframework.exceptions.YadaInvalidUsageException;

public class YadaNotifyData {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	static final String MODAL_VIEW = "/yada/modalNotify";
	private Model model;
	private RedirectAttributes redirectAttributes;
	private String severity = VAL_NOTIFICATION_SEVERITY_OK;
	private String title;
	private String message;
	private MessageSource messageSource;
	private Locale locale;

	// Package visibility
	YadaNotifyData(Model model, MessageSource messageSource, Locale locale) {
		this.model = model;
		this.messageSource = messageSource;
		this.locale = locale==null?LocaleContextHolder.getLocale():locale;
	}
	
	// Package visibility
	YadaNotifyData(RedirectAttributes redirectAttributes, MessageSource messageSource, Locale locale) {
		this.redirectAttributes = redirectAttributes;
		this.messageSource = messageSource;
		this.locale = locale==null?LocaleContextHolder.getLocale():locale;
	}
	
	/**
	 * Makes the notification active. Can be called many times to add different notifications, even on the same instance, after setting a new title/message/severity.
	 * @return If used with a Model, returns the view of the notification modal.
	 */
	public String add() {
		if (redirectAttributes!=null) {
			activateRedirect();
			return null;
		}
		activateNormal();
		return MODAL_VIEW;
	}

	/**
	 * Set the notification title
	 * @param title
	 * @return
	 */
	public YadaNotifyData setTitle(String title) {
		this.title = title;
		return this;
	}

	/**
	 * Set the notification title using localization
	 * @param titleKeyAndArgs the title key, followed by optional arguments to be replaced in the localized value
	 * @throws YadaInvalidUsageException if yadaMessageSource() hasn't been called
	 * @return
	 */
	public YadaNotifyData setTitleKey(String ... titleKeyAndArgs) {
		ensureLocalized();
		String[] argsArray = Arrays.copyOfRange(titleKeyAndArgs, 1, titleKeyAndArgs.length);
		this.title = messageSource.getMessage(titleKeyAndArgs[0], argsArray, locale);
		return this;
	}
		
	/**
	 * Set the notification message. Can be HTML.
	 * @param message
	 * @return
	 */
	public YadaNotifyData message(String message) {
		this.message = message;
		return this;
	}
	
	/**
	 * Set the autoclose time in milliseconds - no close button is rendered
	 * @param milliseconds
	 * @return
	 */
	public YadaNotifyData autoclose(long milliseconds) {
		if (model!=null) {
			model.addAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
		} else {
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_AUTOCLOSE, milliseconds);
		}
		return this;
	}

	/**
	 * Set the page to reload when the modal is closed
	 */
	public YadaNotifyData reloadOnClose() {
		if (model!=null) {
			model.addAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
		} else {
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_RELOADONCLOSE, KEY_NOTIFICATION_RELOADONCLOSE);
		}
		return this;
	}

	/**
	 * Set the notification message using localization. Can be HTML.
	 * @param messageKeyAndArgs the message key, followed by optional arguments to be replaced in the localized value
	 * @throws YadaInvalidUsageException if yadaMessageSource() hasn't been called
	 * @return
	 */
	public YadaNotifyData messageKey(String ... messageKeyAndArgs) {
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
	public YadaNotifyData redirectOnClose(String path) {
		if (model!=null) {
			model.addAttribute(KEY_NOTIFICATION_REDIRECT, path);
		} else {
			redirectAttributes.addFlashAttribute(KEY_NOTIFICATION_REDIRECT, path);
		}
		return this;
	}


	/**
	 * Add a script id to call when opening the notification modal. The script must be inserted into "/script.html"
	 * @param scriptId
	 */
	public YadaNotifyData callScript(String scriptId) {
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
	 * Set the notification severity if active is true
	 * @param active true to set the severity, false for not setting it
	 * @return
	 */
	public YadaNotifyData ok(boolean active) {
		if (active) {
			return this.ok();
		}
		return this;
	}
	
	/**
	 * Set the notification severity if active is true
	 * @param active true to set the severity, false for not setting it
	 * @return
	 */
	public YadaNotifyData info(boolean active) {
		if (active) {
			return this.info();
		}
		return this;
	}
	
	/**
	 * Set the notification severity if active is true
	 * @param active true to set the severity, false for not setting it
	 * @return
	 */
	public YadaNotifyData error(boolean active) {
		if (active) {
			return this.error();
		}
		return this;
	}
	
	/**
	 * Set the notification severity. This is the default.
	 * @return
	 */
	public YadaNotifyData ok() {
		this.severity = VAL_NOTIFICATION_SEVERITY_OK;
		return this;
	}
	
	/**
	 * Set the notification severity
	 * @return
	 */
	public YadaNotifyData info() {
		this.severity = VAL_NOTIFICATION_SEVERITY_INFO;
		return this;
	}
	
	/**
	 * Set the notification severity
	 * @return
	 */
	public YadaNotifyData error() {
		this.severity = VAL_NOTIFICATION_SEVERITY_ERROR;
		return this;
	}
	
	private boolean isLocalized() {
		return messageSource!=null && locale !=null;
	}
	
	private void ensureLocalized() {
		if (!isLocalized()) {
			throw new YadaInvalidUsageException("The Locale must be passed to YadaNotify before using keys");
		}
	}
	
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


}
