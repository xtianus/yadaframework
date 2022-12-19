package net.yadaframework.core;

public interface YadaConstants {
	
	String REQUEST_HASERROR_FLAG = "YADA_HAS_ERROR";

	String SESSION_USER_TIMEZONE = "YADA_USER_TIMEZONE";

	String EMAIL_TEMPLATES_PREFIX = "template"; // Folder in the classpath for email templates (NO LEADING SLASH!)
	// Using a non-common name (like would be "email") so that there can't be mismatch with a user-define view folder
	String EMAIL_TEMPLATES_FOLDER = "email"; // Folder in the classpath for email templates (NO LEADING SLASH!)
	String YADA_VIEW_PREFIX = "net/yadaframework/views"; // Folder in the classpath containing YADA_VIEW_FOLDER (NO LEADING SLASH!)
//	String YADA_VIEW_FOLDER = "yada"; // Folder in the classpath containing yada view snippets (NO LEADING SLASH!)

	/**
	 * Chiave per il messaggio che appare in un dialog modal: testo, severity e contenuto.
	 * 
	 */
	String KEY_NOTIFICATION_TOTALSEVERITY="YADA_TOTSEVERITY"; // La severity più alta tra tutti i messaggi 
	String KEY_NOTIFICATION_TITLE="YADA_NTITLE"; 
	String KEY_NOTIFICATION_BODY="YADA_NBODY"; 
	String KEY_NOTIFICATION_SEVERITY="YADA_SEVERITY"; 
	String KEY_NOTIFICATION_REDIRECT="YADA_REDIRECT"; 
	String KEY_NOTIFICATION_AUTOCLOSE="YADA_AUTOCLOSE"; 
	String KEY_NOTIFICATION_RELOADONCLOSE="YADA_RELOADONCLOSE"; 
	String KEY_NOTIFICATION_CALLSCRIPT="YADA_CALLSCRIPT"; 
	// Keep in sync with the above keys
	String[] KEY_ALL = {
		KEY_NOTIFICATION_TOTALSEVERITY, 
		KEY_NOTIFICATION_TITLE, 
		KEY_NOTIFICATION_BODY, 
		KEY_NOTIFICATION_SEVERITY, 
		KEY_NOTIFICATION_REDIRECT,
		KEY_NOTIFICATION_AUTOCLOSE,
		KEY_NOTIFICATION_RELOADONCLOSE,
		KEY_NOTIFICATION_CALLSCRIPT
	};

	String VAL_NOTIFICATION_SEVERITY_ERROR="error"; 
	String VAL_NOTIFICATION_SEVERITY_OK="ok"; 
	String VAL_NOTIFICATION_SEVERITY_INFO="info"; 
}
