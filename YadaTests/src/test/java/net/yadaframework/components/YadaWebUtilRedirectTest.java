package net.yadaframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import net.yadaframework.core.YadaConfiguration;

/**
 * {@link YadaWebUtil#safeLocalRedirect(String)} keeps local paths, with their query string and fragment, and replaces
 * every value that could leave the site, inject headers or hide such a value behind an encoding with "/".
 */
class YadaWebUtilRedirectTest {
	private final YadaWebUtil yadaWebUtil = new YadaWebUtil();

	@ParameterizedTest
	@ValueSource(strings = {
		"/",
		"/dashboard",
		"/dashboard?tab=users",
		"/dashboard?tab=users&page=2#details",
		"/dashboard#section",
		"/#top",
		"/en/dashboard",
		"/en/dashboard?tab=users",
		"/it_IT/catalog/item-12",
		"/myContext/en/dashboard?tab=users",
		"/search?next=%2Fhome",
		"/search?next=//evil.example",
		"/search?next=https://evil.example",
		"/caf%C3%A9/menu",
		"/a-b_c.d~e/f:g;h=i@j!k$l&m'n(o)p*q+r,s",
		"/files/report.pdf",
		"/.well-known/security.txt",
	})
	void localPathsAreKept(String candidate) {
		assertEquals(candidate, yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t" })
	void missingValuesBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"http://evil.example",
		"https://evil.example/dashboard",
		"HTTPS://evil.example",
		"ftp://evil.example/file",
		"javascript:alert(1)",
		"data:text/html,<script>alert(1)</script>",
		"mailto:someone@evil.example",
		"evil.example",
		"evil.example/dashboard",
		"dashboard",
		"./dashboard",
		"?tab=users",
		"#section",
	})
	void absoluteAndRelativeUrlsBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"//evil.example",
		"//evil.example/dashboard",
		"///evil.example",
		"////evil.example",
		"//evil.example?tab=users",
	})
	void protocolRelativeUrlsBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/\\evil.example",
		"\\\\evil.example",
		"\\/evil.example",
		"/\\/evil.example",
		"/dashboard\\..\\admin",
		"/dashboard?next=\\\\evil.example",
	})
	void backslashesBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		" /dashboard",
		"/dashboard ",
		"/dash board",
		"/\t/evil.example",
		"/\n/evil.example",
		"/\u000B/evil.example",
		"/\u0000/evil.example",
		"/dashboard\u0000",
		"/\u007F/evil.example",
		"/\u0085/evil.example",
		"/ /evil.example",
		"/ /evil.example",
		"/​/evil.example",
		"/﻿/evil.example",
		"/　/evil.example",
		"/dashboard?tab=a b",
	})
	void whitespaceAndControlCharactersBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/dashboard\r\nSet-Cookie: session=evil",
		"/dashboard\rLocation: https://evil.example",
		"/dashboard\nLocation: https://evil.example",
		"/\r\n/evil.example",
		"/dashboard?tab=users\r\nSet-Cookie: session=evil",
		"/dashboard#x\r\nSet-Cookie: session=evil",
	})
	void crlfBecomesRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/%2F/evil.example",
		"/%2f%2fevil.example",
		"/%2F%2Fevil.example/dashboard",
		"/dashboard%2Fadmin",
		"/%5Cevil.example",
		"/%5c%5cevil.example",
		"/%5C/evil.example",
		"/dashboard%5C..%5Cadmin",
		"/%0D%0ASet-Cookie:%20session=evil",
		"/%0d%0a/evil.example",
		"/%09/evil.example",
		"/%00/evil.example",
		"/%20/evil.example",
		"/%E2%80%A8/evil.example",
	})
	void percentEncodedSeparatorsAndControlsBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/%252F/evil.example",
		"/%252f%252fevil.example",
		"/%255Cevil.example",
		"/%255c%255cevil.example",
		"/%250D%250ASet-Cookie:x",
		"/%25252F%25252Fevil.example",
		"/%2525252F%2525252Fevil.example",
		"/%252e%252e/admin",
	})
	void doubleAndMultipleEncodedVariantsBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/..",
		"/../evil.example",
		"/..//evil.example",
		"/dashboard/../../admin",
		"/./dashboard",
		"/%2e%2e/admin",
		"/%2E%2E//evil.example",
		"/%2e/dashboard",
	})
	void dotSegmentsBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/dashboard/{id}",
		"/dashboard?tab={tab}",
		"/dashboard#{x}",
		"/%7Bid%7D/dashboard",
		"/%257Bid%257D",
	})
	void uriTemplateBracesBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}

	/**
	 * A once-encoded "%" decodes to "%" followed by text that is not a valid escape: such paths are rejected by design,
	 * because they can't be told apart from a partial double encoding. A "%25" that decodes to a valid escape of a safe
	 * character is accepted.
	 */
	@Test
	void onceEncodedPercentSigns() {
		assertEquals("/", yadaWebUtil.safeLocalRedirect("/100%25"));
		assertEquals("/", yadaWebUtil.safeLocalRedirect("/100%25off"));
		assertEquals("/a%2541", yadaWebUtil.safeLocalRedirect("/a%2541"));
		assertEquals("/discount?rate=100%25", yadaWebUtil.safeLocalRedirect("/discount?rate=100%25"));
	}

	/**
	 * Pins why a validated value must be used as {@code "redirect:" + safeLocalRedirect(value)} and never passed to
	 * redirectString(): redirectString encodes the path again and prepends the request locale.
	 */
	@Test
	void composedWithRedirectString() {
		YadaConfiguration config = mock(YadaConfiguration.class);
		when(config.isLocalePathVariableEnabled()).thenReturn(true);
		YadaWebUtil configured = new YadaWebUtil();
		ReflectionTestUtils.setField(configured, "config", config);
		// Raw, already encoded value (e.g. a stored getRequestURI()): kept as it is by safeLocalRedirect
		assertEquals("redirect:/caf%C3%A9/menu", "redirect:" + configured.safeLocalRedirect("/caf%C3%A9/menu"));
		// ...but encoded a second time by redirectString
		assertEquals("redirect:/it/caf%25C3%25A9", configured.redirectString(configured.safeLocalRedirect("/caf%C3%A9"), Locale.ITALIAN));
		// A decoded path is accepted, and redirectString is the right formatter for it
		assertEquals("/café", configured.safeLocalRedirect("/café"));
		assertEquals("redirect:/it/caf%C3%A9", configured.redirectString("/café", Locale.ITALIAN));
		// A path that already has a locale keeps it with "redirect:", gets a second one from redirectString
		assertEquals("redirect:/en/x", "redirect:" + configured.safeLocalRedirect("/en/x"));
		assertEquals("redirect:/it/en/x", configured.redirectString(configured.safeLocalRedirect("/en/x"), Locale.ITALIAN));
		assertEquals("redirect:/en/x", configured.redirectString("/en/x", Locale.ENGLISH));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"/100%",
		"/100%2",
		"/bad%zzpath",
		"/%C3%28",
	})
	void malformedEncodingsBecomeRoot(String candidate) {
		assertEquals("/", yadaWebUtil.safeLocalRedirect(candidate));
	}
}
