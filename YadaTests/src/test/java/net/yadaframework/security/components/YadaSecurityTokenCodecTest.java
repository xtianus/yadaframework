package net.yadaframework.security.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.yadaframework.security.components.YadaSecurityTokenCodec.ParsedSecurityToken;

/**
 * The token format, digest and expiry rule of {@link YadaSecurityTokenCodec}.
 */
class YadaSecurityTokenCodecTest {
	private final YadaSecurityTokenCodec codec = new YadaSecurityTokenCodec();

	@Test
	void secretsAre64LowercaseHexAndDifferent() {
		Set<String> secrets = new HashSet<>();
		for (int i = 0; i < 100; i++) {
			String secret = codec.generateSecret();
			assertTrue(secret.matches("[0-9a-f]{64}"), secret);
			secrets.add(secret);
		}
		assertEquals(100, secrets.size());
	}

	@Test
	void theDigestDependsOnPurposeAndSecret() {
		String secret = codec.generateSecret();
		String digest = codec.digest("PASSWORD_RECOVERY", secret);
		assertTrue(digest.matches("[0-9a-f]{64}"), digest);
		assertEquals(digest, codec.digest("PASSWORD_RECOVERY", secret));
		assertNotEquals(digest, codec.digest("REGISTRATION", secret));
		assertNotEquals(digest, codec.digest("PASSWORD_RECOVERY", codec.generateSecret()));
		assertNotEquals(secret, digest);
	}

	@Test
	void matchesChecksPurposeAndSecret() {
		String secret = codec.generateSecret();
		String digest = codec.digest("AUTOLOGIN", secret);
		assertTrue(codec.matches("AUTOLOGIN", secret, digest));
		assertFalse(codec.matches("REGISTRATION", secret, digest));
		assertFalse(codec.matches("AUTOLOGIN", digest, digest), "The stored digest works as a secret");
		assertFalse(codec.matches("AUTOLOGIN", secret, null));
		assertFalse(codec.matches("AUTOLOGIN", null, digest));
	}

	@Test
	void wellFormedLinksAreParsed() {
		String secret = codec.generateSecret();
		ParsedSecurityToken parsed = codec.parse(codec.format(42, secret));
		assertNotNull(parsed);
		assertEquals(42, parsed.id());
		assertEquals(secret, parsed.secret());
		assertFalse(parsed.toString().contains(secret), "toString() shows the secret");
		assertEquals(Long.MAX_VALUE, codec.parse(Long.MAX_VALUE + "-" + secret).id());
	}

	@Test
	void malformedLinksAreRejected() {
		String secret = codec.generateSecret();
		String[] malformed = { null, "", "-", "42", "42-", "-" + secret, "0-" + secret, "042-" + secret, "-42-" + secret, "+42-" + secret,
			"42-" + secret + "-1", "42--" + secret, "42-" + secret.substring(1), "42-" + secret + "0", "42-" + secret.toUpperCase(),
			"42-" + secret.substring(1) + "g", "abc-" + secret, "9223372036854775808-" + secret, "99999999999999999999-" + secret,
			"42-6542972110427423233", " 42-" + secret, "42-" + secret + " ", "42-" + secret + "\n", "42 -" + secret };
		for (String link : malformed) {
			assertNull(codec.parse(link), "Accepted: " + link);
		}
	}

	@Test
	void formatRequiresAPositiveIdAndASecret() {
		assertThrows(IllegalArgumentException.class, () -> codec.format(0, codec.generateSecret()));
		assertThrows(IllegalArgumentException.class, () -> codec.format(1, null));
	}

	@Test
	void aTokenExpiresAtItsExpirationInstant() {
		Date now = new Date();
		assertTrue(YadaSecurityTokenCodec.isExpired(null, now), "No expiration must fail closed");
		assertTrue(YadaSecurityTokenCodec.isExpired(new Date(now.getTime()), now), "now == expiration is expired");
		assertTrue(YadaSecurityTokenCodec.isExpired(new Date(now.getTime() - 1), now));
		assertFalse(YadaSecurityTokenCodec.isExpired(new Date(now.getTime() + 1), now));
	}
}
