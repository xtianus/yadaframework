package net.yadaframework.components;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.net.InetAddresses;

/**
 * Decides which address is the client address of a request.
 * <p>
 * The canonical address is the address of the TCP peer, as given by {@code request.getRemoteAddr()}. Forwarded headers such as
 * {@code X-Forwarded-For} are written by whoever sends the request, so they are only considered when the immediate peer is
 * a configured trusted proxy (an explicit list of IP addresses, never a wildcard), and then only the part of the chain
 * that was appended by trusted proxies is believed: the chain is read from the right-hand end and the first address that is
 * not a trusted proxy is the client. Anything that is not a plain IP address stops the walk and the address of the
 * trusted peer is used.
 * <p>
 * Prefer letting the servlet container do this: with Tomcat's {@code RemoteIpValve} configured with the addresses of the
 * ingress, {@code request.getRemoteAddr()} already is the real client address and no trusted proxy needs to be configured here.
 */
public final class YadaClientAddress {
	/** Maximum length of a forwarded value written to a log */
	public static final int MAX_DIAGNOSTIC_LENGTH = 200;

	private YadaClientAddress() {
		// Static helpers only
	}

	/**
	 * Computes the client address.
	 * @param remoteAddr the address of the immediate peer, from {@code request.getRemoteAddr()}
	 * @param forwardedForValues the values of all the X-Forwarded-For headers of the request, in order; may be null or empty
	 * @param trustedProxies the IP addresses of the proxies allowed to set the forwarded headers; null or empty trusts nobody.
	 * 	Entries that are not IP address literals (e.g. "*" or a host name) are ignored.
	 * @return the client address, or {@code remoteAddr} when the header can not be trusted; never text taken from an untrusted header
	 */
	public static String resolve(String remoteAddr, Collection<String> forwardedForValues, Collection<String> trustedProxies) {
		if (forwardedForValues == null || forwardedForValues.isEmpty()) {
			return remoteAddr;
		}
		Set<String> trusted = normalizeAll(trustedProxies);
		if (trusted.isEmpty() || !trusted.contains(normalize(remoteAddr))) {
			return remoteAddr;
		}
		List<String> chain = new ArrayList<>();
		for (String headerValue : forwardedForValues) {
			if (headerValue != null) {
				for (String hop : headerValue.split(",", -1)) {
					chain.add(hop);
				}
			}
		}
		for (int i = chain.size() - 1; i >= 0; i--) {
			String normalized = normalize(chain.get(i));
			if (normalized == null) {
				return remoteAddr; // Malformed or injected text: the chain is not reliable
			}
			if (!trusted.contains(normalized)) {
				return normalized;
			}
		}
		return remoteAddr; // The whole chain is made of trusted proxies
	}

	/**
	 * Makes a header value safe to write in a log line: control characters (including line breaks) are replaced by "?"
	 * and the length is limited. The result is for diagnostics only and must never be used as an address.
	 * @param value the raw header value, can be null
	 * @return the sanitized text, or null when value is null or blank
	 */
	public static String sanitizeForLog(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		StringBuilder result = new StringBuilder(Math.min(value.length(), MAX_DIAGNOSTIC_LENGTH + 3));
		for (int i = 0; i < value.length() && result.length() < MAX_DIAGNOSTIC_LENGTH; i++) {
			char c = value.charAt(i);
			result.append(Character.isISOControl(c) || (Character.isWhitespace(c) && c != ' ') || Character.getType(c) == Character.FORMAT ? '?' : c);
		}
		if (value.length() > MAX_DIAGNOSTIC_LENGTH) {
			result.append("...");
		}
		return result.toString();
	}

	/**
	 * @return the canonical text of an IP address literal (no host name lookup is done), or null when the text is not one.
	 * 	IPv4-mapped IPv6 addresses are returned in IPv4 form.
	 */
	static String normalize(String text) {
		if (text == null) {
			return null;
		}
		String trimmed = stripWhitespace(text);
		if (!InetAddresses.isInetAddress(trimmed)) {
			return null;
		}
		InetAddress address = InetAddresses.forString(trimmed);
		return InetAddresses.toAddrString(address);
	}

	/**
	 * Removes only spaces and tabs (HTTP optional whitespace) from both ends: String.trim() would also drop control characters
	 */
	private static String stripWhitespace(String text) {
		int start = 0;
		int end = text.length();
		while (start < end && (text.charAt(start) == ' ' || text.charAt(start) == '\t')) {
			start++;
		}
		while (end > start && (text.charAt(end - 1) == ' ' || text.charAt(end - 1) == '\t')) {
			end--;
		}
		return text.substring(start, end);
	}

	private static Set<String> normalizeAll(Collection<String> addresses) {
		if (addresses == null) {
			return Collections.emptySet();
		}
		Set<String> result = new HashSet<>();
		for (String address : addresses) {
			String normalized = normalize(address);
			if (normalized != null) {
				result.add(normalized);
			}
		}
		return result;
	}
}
