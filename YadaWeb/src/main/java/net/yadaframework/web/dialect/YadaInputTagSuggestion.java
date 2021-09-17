package net.yadaframework.web.dialect;

import java.util.Locale;

/**
 * An object implementing this interface can be returned by the <pre> &lt;yada:suggestion yada:listUrl </pre> RequestMapping
 * in order to provide rows for the yada:input suggestion dropdown.
 * Please note that this is not required to implement a suggestion list. See the Yada Framework documentation of the yada:input tag for more info.
 */
public interface YadaInputTagSuggestion {

	/**
	 * Value that will be returned to the backend as the "id" parameter. The "id" name can be changed by implementing the getSuggestionIdRequestName() method
	 * @return
	 */
	String getSuggestionId();

	/**
	 * Text that will be shown to the user in the suggestion list
	 * @param locale the current user locale
	 * @return the text to show in the suggestion list
	 */
	String getSuggestionText(Locale locale);

	/**
	 * Return the name to use as a request parameter when sending the suggestion id. Defaults to "id" when not implemented.
	 * @return
	 */
	default String getSuggestionIdRequestName() {
		return "id";
	}

}
