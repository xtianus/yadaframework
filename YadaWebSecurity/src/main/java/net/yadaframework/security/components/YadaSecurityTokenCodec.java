package net.yadaframework.security.components;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import net.yadaframework.exceptions.YadaInternalException;

/**
 * Generates, digests and parses the bearer tokens of autologin, registration, password recovery and email change links.
 * <p>
 * A token link has the format {@code <id>-<secret>}: the id is the positive database id of the token row, the secret
 * is 32 random bytes from {@link SecureRandom} as 64 lowercase hex characters. Only the SHA-256 digest of a fixed domain
 * prefix, the token purpose and the secret is stored, so a database copy can't be turned into working links. The
 * secret is known only when the token is issued: a token row loaded from the database can't produce its link again.
 * <p>
 * A digest is not a signature: the link is an opaque bearer token, valid only while its row exists, is not expired
 * and has the expected purpose.
 */
@Component
public class YadaSecurityTokenCodec {
	/** Prefix of every digested value, so digests of these tokens can't collide with other SHA-256 uses */
	private static final String DOMAIN_PREFIX = "yada-security-token-v1";
	private static final int SECRET_BYTES = 32;
	/** Positive id without leading zeros, one "-", exactly 64 lowercase hex characters */
	private static final Pattern LINK_PATTERN = Pattern.compile("([1-9][0-9]{0,18})-([0-9a-f]{64})");
	private static final HexFormat HEX = HexFormat.of();

	private final SecureRandom secureRandom = new SecureRandom();

	/**
	 * The two parts of a well-formed token link. {@link #toString()} doesn't show the secret.
	 * @param id the database id of the token row
	 * @param secret the 64 hex characters secret
	 */
	public record ParsedSecurityToken(long id, String secret) {
		@Override
		public String toString() {
			return "ParsedSecurityToken[id=" + id + ", secret=(hidden)]";
		}
	}

	/**
	 * @return a new random secret of 64 lowercase hex characters
	 */
	public String generateSecret() {
		byte[] bytes = new byte[SECRET_BYTES];
		secureRandom.nextBytes(bytes);
		return HEX.formatHex(bytes);
	}

	/**
	 * @param purpose what the token is for, e.g. "AUTOLOGIN" or "PASSWORD_RECOVERY": a secret digested for one purpose
	 * 	never matches the digest of another purpose
	 * @param secret the secret
	 * @return the SHA-256 digest of the domain prefix, purpose and secret, as 64 lowercase hex characters
	 */
	public String digest(String purpose, String secret) {
		if (purpose==null || secret==null) {
			throw new IllegalArgumentException("Purpose and secret are required");
		}
		try {
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			sha256.update((DOMAIN_PREFIX + "\n" + purpose + "\n" + secret).getBytes(StandardCharsets.UTF_8));
			return HEX.formatHex(sha256.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new YadaInternalException("SHA-256 not available", e);
		}
	}

	/**
	 * Checks a presented secret against a stored digest in constant time
	 * @param purpose the expected purpose
	 * @param secret the presented secret
	 * @param storedDigest the stored digest, can be null
	 * @return true when the digest of purpose and secret equals the stored one
	 */
	public boolean matches(String purpose, String secret, String storedDigest) {
		if (purpose==null || secret==null || storedDigest==null) {
			return false;
		}
		byte[] expected = digest(purpose, secret).getBytes(StandardCharsets.US_ASCII);
		return MessageDigest.isEqual(expected, storedDigest.getBytes(StandardCharsets.US_ASCII));
	}

	/**
	 * Parses a token link. Nothing is logged, because the value may carry a secret.
	 * @param link the {@code <id>-<secret>} value, e.g. a path variable
	 * @return the parts, or null when the value is not exactly a positive id, one "-" and a 64 lowercase hex secret
	 */
	public ParsedSecurityToken parse(String link) {
		if (link==null || link.length()>19+1+64) {
			return null;
		}
		Matcher matcher = LINK_PATTERN.matcher(link);
		if (!matcher.matches()) {
			return null;
		}
		try {
			return new ParsedSecurityToken(Long.parseLong(matcher.group(1)), matcher.group(2));
		} catch (NumberFormatException e) {
			return null; // More than Long.MAX_VALUE
		}
	}

	/**
	 * @param id the token row id
	 * @param secret the secret
	 * @return the {@code <id>-<secret>} link part
	 */
	public String format(long id, String secret) {
		if (id<1 || secret==null) {
			throw new IllegalArgumentException("A positive id and a secret are required");
		}
		return id + "-" + secret;
	}

	/**
	 * The expiration rule of every token: a token is expired from its expiration instant on, and a missing expiration
	 * counts as expired (fail closed).
	 * @param expiration the expiration, can be null
	 * @param now the current time
	 * @return true when the token can't be used any more
	 */
	public static boolean isExpired(Date expiration, Date now) {
		return expiration==null || now.getTime()>=expiration.getTime();
	}
}
