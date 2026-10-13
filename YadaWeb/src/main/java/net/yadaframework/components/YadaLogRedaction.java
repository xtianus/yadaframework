package net.yadaframework.components;

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
	/** Request parameters whose value is a secret */
	private static final Pattern SECRET_PARAMETER = Pattern.compile("((?:^|[?&;])(?:token|password|confirmPassword|newPassword|_csrf)=)[^&#\\s]*");

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
	 * Hides the token links and the values of the token, password and CSRF parameters of a query string
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
