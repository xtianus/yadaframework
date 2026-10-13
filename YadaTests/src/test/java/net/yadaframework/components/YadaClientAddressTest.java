package net.yadaframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import net.yadaframework.core.YadaConfiguration;

/**
 * The client address is the TCP peer address unless a configured trusted proxy forwards it, and an
 * X-Forwarded-For header is never copied into the address as text.
 */
class YadaClientAddressTest {
	private static final List<String> PROXIES = List.of("10.0.0.1", "10.0.0.2");

	private static String resolve(String peer, String header, List<String> trusted) {
		return YadaClientAddress.resolve(peer, header == null ? null : List.of(header), trusted);
	}

	@Test
	void directClientSpoofingTheHeaderIsIgnored() {
		assertEquals("198.51.100.7", resolve("198.51.100.7", "203.0.113.9", PROXIES));
		assertEquals("198.51.100.7", resolve("198.51.100.7", "10.0.0.1", PROXIES), "A trusted address in the header does not make the peer trusted");
		assertEquals("198.51.100.7", resolve("198.51.100.7", "203.0.113.9", Collections.emptyList()));
		assertEquals("198.51.100.7", resolve("198.51.100.7", "203.0.113.9", null));
	}

	@Test
	void noHeaderMeansThePeer() {
		assertEquals("10.0.0.1", resolve("10.0.0.1", null, PROXIES));
		assertEquals("10.0.0.1", YadaClientAddress.resolve("10.0.0.1", Collections.emptyList(), PROXIES));
	}

	@Test
	void trustedProxyForwardsTheClient() {
		assertEquals("203.0.113.9", resolve("10.0.0.1", "203.0.113.9", PROXIES));
		assertEquals("203.0.113.9", resolve("10.0.0.1", " 203.0.113.9 ", PROXIES));
		assertEquals("2001:db8::1", resolve("10.0.0.1", "2001:DB8:0:0:0:0:0:1", PROXIES), "The canonical text is returned");
	}

	@Test
	void chainIsReadFromTheTrustedRightHandEnd() {
		// The client claims to be 1.1.1.1, the first proxy saw 203.0.113.9, the second proxy is trusted too
		assertEquals("203.0.113.9", resolve("10.0.0.1", "1.1.1.1, 203.0.113.9, 10.0.0.2", PROXIES));
		assertEquals("203.0.113.9", resolve("10.0.0.1", "10.0.0.2, 203.0.113.9, 10.0.0.2", PROXIES));
		// An untrusted hop stops the walk: what is on its left was written by an untrusted party
		assertEquals("203.0.113.9", resolve("10.0.0.1", "1.1.1.1, 203.0.113.9", PROXIES));
		assertEquals("10.0.0.1", resolve("10.0.0.1", "10.0.0.2", PROXIES), "Only trusted proxies in the chain");
	}

	@Test
	void multipleHeaderLinesAreOneChain() {
		assertEquals("203.0.113.9", YadaClientAddress.resolve("10.0.0.1", List.of("1.1.1.1", "203.0.113.9, 10.0.0.2"), PROXIES));
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"unknown", "not an ip", "<script>", "203.0.113.9:8080", "[2001:db8::1]", "999.1.1.1", "1.1.1", "evil.example.com",
		"203.0.113.9\nInjected line", "203.0.113.9\r\nInjected", "203.0.113.9\u0000", "203.0.113.9; DROP", "", " ", ",", "1.1.1.1,,10.0.0.2"
	})
	void malformedChainFallsBackToThePeer(String header) {
		assertEquals("10.0.0.1", resolve("10.0.0.1", header, PROXIES));
		assertEquals("10.0.0.1", resolve("10.0.0.1", "203.0.113.9, " + header, PROXIES), "A malformed hop is a malformed chain");
	}

	@ParameterizedTest
	@ValueSource(strings = { "*", "0.0.0.0/0", "0.0.0.0", "all", "any", "10.0.0.0/8", "localhost", "" })
	void trustAllIsNotPossible(String entry) {
		assertEquals("10.0.0.1", resolve("10.0.0.1", "203.0.113.9", List.of(entry)));
		assertEquals("198.51.100.7", resolve("198.51.100.7", "203.0.113.9", List.of(entry)));
	}

	@Test
	void addressesAreComparedByValue() {
		assertEquals("203.0.113.9", resolve("0:0:0:0:0:0:0:1", "203.0.113.9", List.of("::1")));
		assertEquals("203.0.113.9", resolve("::ffff:10.0.0.1", "203.0.113.9", List.of("10.0.0.1")));
	}

	@Test
	void sanitizedDiagnosticTextHasNoControlCharacters() {
		String sanitized = YadaClientAddress.sanitizeForLog("1.1.1.1\r\nX-Injected: yes\u0000\t end‮");
		assertFalse(sanitized.chars().anyMatch(c -> c < 32 || c == 0x2028 || c == 0x202e || c == 127), sanitized);
		assertTrue(sanitized.startsWith("1.1.1.1??X-Injected: yes"));
		assertNull(YadaClientAddress.sanitizeForLog(null));
		assertNull(YadaClientAddress.sanitizeForLog("  "));
		String longValue = "1".repeat(1000);
		assertEquals(YadaClientAddress.MAX_DIAGNOSTIC_LENGTH + 3, YadaClientAddress.sanitizeForLog(longValue).length());
	}

	private YadaWebUtil webUtil(List<String> trustedProxies) {
		YadaWebUtil yadaWebUtil = new YadaWebUtil();
		YadaConfiguration config = mock(YadaConfiguration.class);
		when(config.getTrustedProxies()).thenReturn(trustedProxies);
		ReflectionTestUtils.setField(yadaWebUtil, "config", config);
		return yadaWebUtil;
	}

	private MockHttpServletRequest request(String peer, String forwardedFor) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRemoteAddr(peer);
		if (forwardedFor != null) {
			request.addHeader("X-Forwarded-For", forwardedFor);
		}
		return request;
	}

	@Test
	void getClientAddressIgnoresTheHeaderByDefault() {
		YadaWebUtil yadaWebUtil = webUtil(Collections.emptyList());
		assertEquals("198.51.100.7", yadaWebUtil.getClientAddress(request("198.51.100.7", "203.0.113.9")));
		assertEquals("10.0.0.1", yadaWebUtil.getClientAddress(request("10.0.0.1", "203.0.113.9")), "Nobody is trusted unless configured");
	}

	@Test
	void getClientAddressFollowsTrustedProxiesOnly() {
		YadaWebUtil yadaWebUtil = webUtil(PROXIES);
		assertEquals("203.0.113.9", yadaWebUtil.getClientAddress(request("10.0.0.1", "6.6.6.6, 203.0.113.9")));
		assertEquals("198.51.100.7", yadaWebUtil.getClientAddress(request("198.51.100.7", "203.0.113.9")));
		assertEquals("10.0.0.1", yadaWebUtil.getClientAddress(request("10.0.0.1", "evil\nline")));
	}

	@Test
	@SuppressWarnings("deprecation")
	void getClientIpReturnsTheAddressNeverTheHeaderText() {
		YadaWebUtil yadaWebUtil = webUtil(Collections.emptyList());
		assertEquals("198.51.100.7", yadaWebUtil.getClientIp(request("198.51.100.7", "203.0.113.9\r\nforged")));
		assertEquals("?", yadaWebUtil.getClientIp(request("", null)));
	}
}
