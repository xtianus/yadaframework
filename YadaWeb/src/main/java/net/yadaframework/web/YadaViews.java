package net.yadaframework.web;

/**
 * Some view strings that have specific effects when returned from a @Controller
 *
 */
public interface YadaViews {
	/**
	 * Empty HTML, useful sometimes for deletions
	 */
	String VIEW_EMPTY = "/yada/empty";

	/**
	 * Ajax method causes a browser redirect. Set the "targetUrl" attribute in the model.
	 */
	String AJAX_REDIRECT = "/yada/ajaxRedirect";

	/**
	 * The name of the attribute to set with the full target redirect url
	 */
	String AJAX_REDIRECT_URL = "targetUrl";

	/**
	 * The name of the attribute to set with the relative target redirect url: it will be put inside @{ }
	 */
	String AJAX_REDIRECT_URL_RELATIVE = "targetUrlRelative";

	/**
	 * Set this attribute to true to open the redirect page in a new tab
	 */
	String AJAX_REDIRECT_NEWTAB = "newTab";

	/**
	 * Ajax method returns cleanly.
	 */
	String AJAX_SUCCESS = "/yada/ajaxSuccess";

	/**
	 * Ajax method not allowed.
	 */
	String AJAX_FORBIDDEN = "/yada/ajaxForbidden"; // NEVER USED?

	/**
	 * Perform a page reload when returning from ajax method
	 */
	String AJAX_RELOAD = "/yada/ajaxReload";

	/**
	 * Open a notification modal when returning from an ajax request.
	 * The notification must have been saved using the YadaNotify API before returning.
	 * This is the default modal. You can customize it with config/paths/notificationModalView
	 */
	String AJAX_NOTIFY = "/yada/modalNotify";
	String AJAX_NOTIFY_B4 = "/yada/modalNotifyB4";
	String AJAX_NOTIFY_B3 = "/yada/modalNotifyB3";

	/**
	 * Close any open modal
	 */
	String AJAX_CLOSE_MODAL = "/yada/ajaxCloseModal";

	/**
	 * When returning from the ajax method, a "server error" modal is shown. A custom error description
	 * can be added as a model attribute named "errorDescription" before returning.
	 */
	String AJAX_SERVER_ERROR = "/yada/ajaxError";

	/**
	 * Model attribute that contains the custom error description
	 */
	String AJAX_SERVER_ERROR_DESCRIPTION = "errorDescription";

	/**
	 * HTML that shows suggestions, to be used with the yada:input and yada:suggestion tags
	 */
	String AJAX_SUGGESTION_FRAGMENT = "/yada/formfields/inputSuggestionFragment :: fragment";
}
