package net.yadaframework.tests.web.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.tests.support.ApplicationSession;

/**
 * With the default configuration (no trusted proxy) a client connected directly to Tomcat can not choose the audit
 * address by sending X-Forwarded-For: the logged "remoteIp" is the TCP peer, and the header text is kept apart.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditDirectPeerTest {
	private ApplicationSession session;
	private AuditRequestProbe probe;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(run -> { }, application -> () -> { });
		probe = new AuditRequestProbe(session);
	}

	@AfterAll
	void stopApplication() {
		if (session != null) {
			session.close();
		}
	}

	private static boolean isLoopback(String address) {
		return "127.0.0.1".equals(address) || "0:0:0:0:0:0:0:1".equals(address) || "::1".equals(address);
	}

	@Test
	void requestWithoutHeaderIsLoggedWithThePeerAddress() throws Exception {
		Map<String, String> mdc = probe.get();
		assertTrue(isLoopback(mdc.get("remoteIp")), mdc.toString());
		assertNull(mdc.get("forwardedFor"));
	}

	@Test
	void spoofedHeaderDoesNotReplaceThePeerAddress() throws Exception {
		Map<String, String> mdc = probe.get("203.0.113.9");
		String remoteIp = mdc.get("remoteIp");
		assertTrue(isLoopback(remoteIp), "remoteIp=" + remoteIp);
		assertEquals("203.0.113.9", mdc.get("forwardedFor"));
	}

	@Test
	void multiHopAndTextHeadersDoNotReplaceThePeerAddress() throws Exception {
		for (String[] values : new String[][] { { "1.1.1.1, 2.2.2.2, 127.0.0.1" }, { "[for 1.1.1.1]" }, { "evil <b>text</b>" }, { "1.1.1.1", "2.2.2.2" } }) {
			Map<String, String> mdc = probe.get(values);
			String remoteIp = mdc.get("remoteIp");
			assertTrue(isLoopback(remoteIp), "remoteIp=" + remoteIp + " for " + String.join(" | ", values));
		}
	}

	@Test
	void controlCharactersInTheHeaderNeverReachTheLog() throws Exception {
		// The container may reject the request outright; if it reaches the filter the MDC must be clean
		Map<String, String> mdc = probe.getRaw("203.0.113.9\u0001\u007fINFO forged");
		if (mdc != null) {
			for (String field : new String[] { "remoteIp", "forwardedFor" }) {
				String value = mdc.get(field);
				if (value != null) {
					assertFalse(value.chars().anyMatch(c -> c < 32 || c == 127), field + "=" + value);
				}
			}
			assertTrue(isLoopback(mdc.get("remoteIp")));
		}
	}
}
