package net.yadaframework.tests.web.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.tests.support.ApplicationSession;

/**
 * When the loopback peer (the test client) is configured as a trusted proxy, the audit address follows the
 * X-Forwarded-For chain from its right-hand end; malformed chains still fall back to the peer address.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditConfiguredProxyTest {
	private ApplicationSession session;
	private AuditRequestProbe probe;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(run -> run.setConfigurationValue("security/trustedProxies/proxy", "127.0.0.1", "::1"), application -> () -> { });
		probe = new AuditRequestProbe(session);
	}

	@AfterAll
	void stopApplication() {
		if (session != null) {
			session.close();
		}
	}

	@Test
	void trustedPeerForwardsTheClientAddress() throws Exception {
		Map<String, String> mdc = probe.get("203.0.113.9");
		assertEquals("203.0.113.9", mdc.get("remoteIp"));
		assertEquals("203.0.113.9", mdc.get("forwardedFor"));
	}

	@Test
	void chainIsReadFromTheRightHandEnd() throws Exception {
		Map<String, String> mdc = probe.get("6.6.6.6, 203.0.113.9, 127.0.0.1");
		assertEquals("203.0.113.9", mdc.get("remoteIp"), "The client-supplied left-hand entry is not believed");
	}

	@Test
	void malformedChainFallsBackToThePeerAddress() throws Exception {
		Map<String, String> mdc = probe.get("203.0.113.9, not-an-address");
		String remoteIp = mdc.get("remoteIp");
		assertTrue("127.0.0.1".equals(remoteIp) || "0:0:0:0:0:0:0:1".equals(remoteIp) || "::1".equals(remoteIp), "remoteIp=" + remoteIp);
	}
}
