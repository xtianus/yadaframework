package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import net.yadaframework.components.YadaClientAddress;
import net.yadaframework.tests.support.LogCapture;

/**
 * The audit address (MDC "remoteIp") is the TCP peer address; X-Forwarded-For only counts when it comes from a trusted
 * proxy, and its text never reaches the address field or injects line breaks. Token redaction of the endpoint is kept.
 */
class AuditFilterSecurityTest {

	/**
	 * Filter with a fixed trusted proxy list, as the configuration would provide
	 */
	private static class TestAuditFilter extends AuditFilter {
		private final List<String> trusted;
		TestAuditFilter(List<String> trusted) {
			this.trusted = trusted;
		}
		@Override
		protected Collection<String> getTrustedProxies() {
			return trusted;
		}
	}

	/**
	 * @return the MDC seen inside the filter chain
	 */
	private Map<String, String> mdcSeen(AuditFilter filter, MockHttpServletRequest request) throws Exception {
		Map<String, String> seen = new HashMap<>();
		MockFilterChain chain = new MockFilterChain() {
			@Override
			public void doFilter(jakarta.servlet.ServletRequest servletRequest, jakarta.servlet.ServletResponse servletResponse) {
				Map<String, String> copy = MDC.getCopyOfContextMap();
				if (copy != null) {
					seen.putAll(copy);
				}
			}
		};
		filter.doFilter(request, new MockHttpServletResponse(), chain);
		return seen;
	}

	private MockHttpServletRequest request(String peer, String... forwardedFor) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/page");
		request.setRemoteAddr(peer);
		for (String value : forwardedFor) {
			request.addHeader("X-Forwarded-For", value);
		}
		return request;
	}

	@Test
	void directClientCanNotSpoofTheAddress() throws Exception {
		Map<String, String> mdc = mdcSeen(new TestAuditFilter(List.of("10.0.0.1")), request("198.51.100.7", "203.0.113.9"));
		assertEquals("198.51.100.7", mdc.get("remoteIp"));
		assertEquals("203.0.113.9", mdc.get("forwardedFor"), "The header stays visible as a diagnostic");
	}

	@Test
	void nobodyIsTrustedByDefault() throws Exception {
		Map<String, String> mdc = mdcSeen(new TestAuditFilter(Collections.emptyList()), request("10.0.0.1", "203.0.113.9"));
		assertEquals("10.0.0.1", mdc.get("remoteIp"));
	}

	@Test
	void noConfigurationMeansNobodyIsTrusted() throws Exception {
		// The real getTrustedProxies() finds no YadaConfiguration bean here
		Map<String, String> mdc = mdcSeen(new AuditFilter(), request("10.0.0.1", "203.0.113.9"));
		assertEquals("10.0.0.1", mdc.get("remoteIp"));
	}

	@Test
	void trustedProxyForwardsTheClient() throws Exception {
		Map<String, String> mdc = mdcSeen(new TestAuditFilter(List.of("10.0.0.1", "10.0.0.2")), request("10.0.0.1", "1.1.1.1, 203.0.113.9, 10.0.0.2"));
		assertEquals("203.0.113.9", mdc.get("remoteIp"));
		assertEquals("1.1.1.1, 203.0.113.9, 10.0.0.2", mdc.get("forwardedFor"));
	}

	@Test
	void untrustedPeerWithTrustedLookingChainIsIgnored() throws Exception {
		Map<String, String> mdc = mdcSeen(new TestAuditFilter(List.of("10.0.0.1")), request("198.51.100.7", "203.0.113.9, 10.0.0.1"));
		assertEquals("198.51.100.7", mdc.get("remoteIp"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "evil text", "203.0.113.9\r\nINFO forged log line", "203.0.113.9\nX", "\u0000", "203.0.113.9:80", "<script>alert(1)</script>" })
	void untrustedHeaderTextNeverReachesTheAddressAndHasNoLineBreaks(String header) throws Exception {
		for (List<String> trusted : List.of(Collections.<String>emptyList(), List.of("10.0.0.1"))) {
			Map<String, String> mdc = mdcSeen(new TestAuditFilter(trusted), request("10.0.0.1", header));
			assertEquals("10.0.0.1", mdc.get("remoteIp"));
			for (String field : List.of("remoteIp", "forwardedFor")) {
				String value = mdc.get(field);
				if (value != null) {
					assertFalse(value.chars().anyMatch(c -> c < 32 || c == 127), field + " contains a control character: " + value);
				}
			}
		}
	}

	@Test
	void longHeaderIsTruncatedInTheDiagnosticField() throws Exception {
		Map<String, String> mdc = mdcSeen(new TestAuditFilter(Collections.emptyList()), request("10.0.0.1", "9".repeat(5000)));
		assertEquals(YadaClientAddress.MAX_DIAGNOSTIC_LENGTH + 3, mdc.get("forwardedFor").length());
	}

	@Test
	void missingPeerAddressIsAQuestionMark() throws Exception {
		Map<String, String> mdc = mdcSeen(new TestAuditFilter(Collections.emptyList()), request(""));
		assertEquals("?", mdc.get("remoteIp"));
		assertNull(mdc.get("forwardedFor"));
	}

	@Test
	void endpointTokensAreStillRedacted() throws Exception {
		MockHttpServletRequest request = request("10.0.0.1");
		request.setRequestURI("/autologin/12345678901234567890123456789012345678901234");
		Map<String, String> mdc = mdcSeen(new TestAuditFilter(Collections.emptyList()), request);
		assertNotNull(mdc.get("endpoint"));
		assertFalse(mdc.get("endpoint").contains("12345678901234567890123456789012345678901234"), mdc.get("endpoint"));
	}

	/**
	 * The DEBUG dump of the request parameters hides every secret parameter known to YadaLogRedaction, also when
	 * the name is a nested property path
	 */
	@Test
	void debugParameterDumpHidesSecrets() throws Exception {
		Logger auditLogger = (Logger) LoggerFactory.getLogger(AuditFilter.class);
		Level previousLevel = auditLogger.getLevel();
		auditLogger.setLevel(Level.DEBUG);
		try (LogCapture logs = LogCapture.start()) {
			MockHttpServletRequest request = new MockHttpServletRequest("POST", "/dashboard/user/save");
			request.setRemoteAddr("10.0.0.1");
			request.addParameter("userCredentials.username", "visible-user");
			request.addParameter("userCredentials.newPassword", "secret-new-password");
			request.addParameter("newPassword", "secret-plain-new-password");
			request.addParameter("password", "secret-password");
			request.addParameter("confirmPassword", "secret-confirm-password");
			request.addParameter("token", "secret-token");
			request.addParameter("_csrf", "secret-csrf");
			mdcSeen(new TestAuditFilter(Collections.emptyList()), request);
			String text = logs.text();
			assertTrue(text.contains("visible-user"), "The parameter dump did not run:\n" + text);
			assertFalse(text.contains("secret-"), text);
		} finally {
			auditLogger.setLevel(previousLevel);
		}
	}

	@Test
	void mdcIsCleanedAfterTheRequest() throws Exception {
		mdcSeen(new TestAuditFilter(Collections.emptyList()), request("10.0.0.1", "203.0.113.9"));
		assertNull(MDC.get("remoteIp"));
		assertNull(MDC.get("forwardedFor"));
	}
}
