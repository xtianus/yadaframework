package net.yadaframework.web;

/**
 * Some view strings that have specific effects when returned from a @Controller
 *
 */
public interface YadaViews {

	/**
	 * Ajax method returns cleanly.
	 */
	String AJAX_SUCCESS = "/yada/ajaxSuccess";
	
	/**
	 * Perform a page reload when returning from ajax method
	 */
	String AJAX_RELOAD = "/yada/ajaxReload";
	
	/**
	 * Open a notification modal when returning from an ajax request.
	 * The notification must have been saved using the YadaNotify API before returning.
	 */
	String AJAX_NOTIFY = "/yada/modalNotify";
	
	/**
	 * Close any open modal
	 */
	String AJAX_CLOSE_MODAL = "/yada/ajaxCloseModal";

	/**
	 * When returning from the ajax method, a "server error" modal is shown. A custom error description
	 * can be added as a model attribute named "errorDescription" before returning.
	 */
	String AJAX_SERVER_ERROR = "/yada/ajaxError";
}
