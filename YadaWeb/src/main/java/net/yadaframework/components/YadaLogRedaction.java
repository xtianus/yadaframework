package net.yadaframework.components;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes bearer secrets from text that is going to be logged: request urls, query strings and email bodies.
 * <p>
 * Autologin, registration, password recovery and email change links carry a token "&lt;id>-&lt;secret>" in their path.
 * The secret is replaced with "[hidden]" wherever a token appears, and the whole path segment after the framework token
 * routes is hidden even when malformed, so that near-valid or legacy tokens are not logged either. The id stays, to
 * correlate log lines.
 */
public final class YadaLogRedaction {
	/** The replacement of a secret */
	public static final String HIDDEN = "[hidden]";
	/** A token: id, "-", then 64 hex characters (current format) or 8+ digits (legacy numeric format) */
	private static final Pattern TOKEN = Pattern.compile("(?<![0-9A-Za-z])(\\d+)-(?:[0-9a-fA-F]{64}|\\d{8,})(?![0-9A-Za-z])");
	/** The path segment after the routes that receive a token */
	private static final Pattern TOKEN_ROUTE = Pattern.compile("(/(?:autologin|registrationConfirmation|passwordReset|changeEmailConfirm|yadaPasswordChangeModal)/)(?!\\d+-" + Pattern.quote(HIDDEN) + "(?:[/?#;\\s\"'<]|$))[^/?#;\\s\"'<]+");
	/** The names of the request parameters whose value is a secret, also as the last segment of a property path like "userCredentials.newPassword" */
	private static final List<String> SECRET_PARAMETER_NAMES = List.of("token", "password", "confirmPassword", "newPassword", "_csrf");
	/** A secret parameter in a query string */
	private static final Pattern SECRET_PARAMETER = Pattern.compile("((?:^|[?&;])(?:[^&#;=\\s]*\\.)?(?:" + String.join("|", SECRET_PARAMETER_NAMES) + ")=)[^&#\\s]*");

	private YadaLogRedaction() {
	}

	/**
	 * Hides the secret of every token link in a text
	 * @param text a url, a path or any text, can be null
	 * @return the text without secrets, null for null
	 */
	public static String redactTokens(String text) {
		if (text==null) {
			return null;
		}
		String result = TOKEN.matcher(text).replaceAll("$1-" + Matcher.quoteReplacement(HIDDEN));
		return TOKEN_ROUTE.matcher(result).replaceAll("$1" + Matcher.quoteReplacement(HIDDEN));
	}

	/**
	 * Tells if the value of a request parameter is a secret that must not be logged: the token, password, confirmPassword,
	 * newPassword and _csrf parameters, also as the last segment of a property path such as "userCredentials.newPassword".
	 * {@link #redactQueryString(String)} hides the same parameters.
	 * @param parameterName the name of the parameter, can be null
	 * @return true when the value must be hidden
	 */
	public static boolean isSecretParameter(String parameterName) {
		if (parameterName==null) {
			return false;
		}
		String lastSegment = parameterName.substring(parameterName.lastIndexOf('.')+1);
		return SECRET_PARAMETER_NAMES.contains(lastSegment);
	}

	/**
	 * Hides the token links and the values of the token, password and CSRF parameters of a query string, also when the
	 * parameter name is a property path (see {@link #isSecretParameter(String)})
	 * @param queryString the query string, can be null
	 * @return the query string without secrets, null for null
	 */
	public static String redactQueryString(String queryString) {
		if (queryString==null) {
			return null;
		}
		return SECRET_PARAMETER.matcher(redactTokens(queryString)).replaceAll("$1" + Matcher.quoteReplacement(HIDDEN));
	}
}
