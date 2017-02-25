package net.yadaframework.web;
import java.io.File;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YadaEmailParam {
	/**
	 * The email to show as sender. If null it will get its value from config.getEmailFrom()
	 */
	public String fromEmail;
	/**
	 * The recipient email(s), must not be null or empty
	 */
	public String[] toEmail;
	/**
	 * The replyTo email. Can be null.
	 */
	public String replyTo;
	/**
	 * Email template name, which is the html file with no extension nor localization suffix
	 */
	public String emailName;
	/**
	 * Values for subject string parameters - can be null when not used
	 */
	public Object[] subjectParams;
	/**
	 * Thymeleaf Model attributes to use in the template - can be null
	 */
	public Map<String, Object> templateParams;
	/**
	 * mappa chiave-valore di immagini inline di tipo "cid:". Il valore è un path relativo al context-path, come per esempio "/res/img/pippo.jpg"
	 */
	public Map<String, String> inlineResources;
	/**
	 * mappa filename-File di file da inviare come attachment. Il filename deve avere la giusta estensione per avere il corretto mime type. 
	 */
	public Map<String, File> attachments;
	public Locale locale;
	/**
	 * Add a timestamp to the subject - defaults to false
	 */
	public boolean addTimestamp=false;
}
