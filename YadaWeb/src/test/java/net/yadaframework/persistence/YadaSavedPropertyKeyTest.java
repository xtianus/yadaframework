package net.yadaframework.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.yadaframework.exceptions.YadaInvalidValueException;

/**
 * Verifies saved-property key normalization, validation, and default state.
 */
class YadaSavedPropertyKeyTest {

	/**
	 * Verifies that missing and blank scopes use the shared default scope.
	 */
	@Test
	void missingApplicationNamesUseDefaultScope() {
		assertEquals("default", YadaSavedPropertyKey.DEFAULT_APPLICATION_NAME);
		assertEquals("default", YadaSavedPropertyKey.normalizeApplicationName(null));
		assertEquals("default", YadaSavedPropertyKey.normalizeApplicationName(""));
		assertEquals("default", YadaSavedPropertyKey.normalizeApplicationName(" \t "));
		assertEquals("default", YadaSavedPropertyKey.of(null, "name", YadaSavedPropertyCodecs.stringCodec()).getApplicationName());
	}

	/**
	 * Verifies that explicit scopes are trimmed and otherwise retained.
	 */
	@Test
	void explicitApplicationNamesAreTrimmedAndRetained() {
		assertEquals("MyApplication", YadaSavedPropertyKey.normalizeApplicationName("  MyApplication  "));
		assertEquals("MyApplication", YadaSavedPropertyKey.of("  MyApplication  ", "name", YadaSavedPropertyCodecs.stringCodec()).getApplicationName());
	}

	/**
	 * Verifies that names are trimmed without changing their case.
	 */
	@Test
	void namesAreTrimmedAndRemainCaseSensitive() {
		YadaSavedPropertyKey<String> uppercaseKey = YadaSavedPropertyKey.of("app", "  Feature.Enabled  ", YadaSavedPropertyCodecs.stringCodec());
		YadaSavedPropertyKey<String> lowercaseKey = YadaSavedPropertyKey.of("app", "feature.enabled", YadaSavedPropertyCodecs.stringCodec());

		assertEquals("Feature.Enabled", uppercaseKey.getName());
		assertEquals("feature.enabled", lowercaseKey.getName());
		assertFalse(uppercaseKey.getName().equals(lowercaseKey.getName()));
	}

	/**
	 * Verifies that null and blank property names are rejected.
	 */
	@Test
	void blankNamesAreRejected() {
		YadaSavedPropertyCodec<String> codec = YadaSavedPropertyCodecs.stringCodec();

		assertThrows(YadaInvalidValueException.class, () -> YadaSavedPropertyKey.of("app", null, codec));
		assertThrows(YadaInvalidValueException.class, () -> YadaSavedPropertyKey.of("app", "", codec));
		assertThrows(YadaInvalidValueException.class, () -> YadaSavedPropertyKey.of("app", " \t ", codec));
	}

	/**
	 * Verifies that scope and property-name storage limits are enforced.
	 */
	@Test
	void overlengthNamesAreRejected() {
		YadaSavedPropertyCodec<String> codec = YadaSavedPropertyCodecs.stringCodec();

		assertThrows(YadaInvalidValueException.class, () -> YadaSavedPropertyKey.of("a".repeat(65), "name", codec));
		assertThrows(YadaInvalidValueException.class, () -> YadaSavedPropertyKey.of("app", "n".repeat(192), codec));
		assertEquals(64, YadaSavedPropertyKey.of("a".repeat(64), "name", codec).getApplicationName().length());
		assertEquals(191, YadaSavedPropertyKey.of("app", "n".repeat(191), codec).getName().length());
	}

	/**
	 * Verifies that codecs and declared defaults cannot be null.
	 */
	@Test
	void nullCodecsAndDefaultsAreRejected() {
		assertThrows(NullPointerException.class, () -> YadaSavedPropertyKey.of("app", "name", null));
		assertThrows(NullPointerException.class, () -> YadaSavedPropertyKey.withDefault("app", "name", YadaSavedPropertyCodecs.stringCodec(), null));
	}

	/**
	 * Verifies getters and absent-default state for a key without a default.
	 */
	@Test
	void keyWithoutDefaultReportsItsState() {
		YadaSavedPropertyCodec<Long> codec = YadaSavedPropertyCodecs.longCodec();
		YadaSavedPropertyKey<Long> key = YadaSavedPropertyKey.of("app", "counter", codec);

		assertEquals("app", key.getApplicationName());
		assertEquals("counter", key.getName());
		assertSame(codec, key.getCodec());
		assertTrue(key.getDefaultValue().isEmpty());
	}

	/**
	 * Verifies getters and present-default state for a key with a default.
	 */
	@Test
	void keyWithDefaultReportsItsState() {
		YadaSavedPropertyCodec<Boolean> codec = YadaSavedPropertyCodecs.booleanCodec();
		YadaSavedPropertyKey<Boolean> key = YadaSavedPropertyKey.withDefault("app", "enabled", codec, Boolean.TRUE);

		assertEquals("app", key.getApplicationName());
		assertEquals("enabled", key.getName());
		assertSame(codec, key.getCodec());
		assertEquals(Boolean.TRUE, key.getDefaultValue().orElseThrow());
	}
}
