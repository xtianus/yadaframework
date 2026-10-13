package net.yadaframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * {@link YadaLogRedaction} hides token secrets in urls, query strings and email bodies.
 */
class YadaLogRedactionTest {
	private static final String SECRET = "52a3d4f399ee681d15726d58b0efa0f0552a3d4f399ee681d15726d58b0efa0f";

	@Test
	void tokenSecretsAreHiddenAndIdsKept() {
		assertEquals("/autologin/12-[hidden]", YadaLogRedaction.redactTokens("/autologin/12-" + SECRET));
		assertEquals("/site/en/registrationConfirmation/3-[hidden]", YadaLogRedaction.redactTokens("/site/en/registrationConfirmation/3-" + SECRET));
		assertEquals("/passwordReset/3-[hidden]", YadaLogRedaction.redactTokens("/passwordReset/3-6542972110427423233"));
		assertEquals("/custom/confirm/7-[hidden]?x=1", YadaLogRedaction.redactTokens("/custom/confirm/7-" + SECRET + "?x=1"));
		String body = "<a href=\"http://localhost/changeEmailConfirm/5-" + SECRET + "\">http://localhost/changeEmailConfirm/5-" + SECRET + "</a>";
		assertFalse(YadaLogRedaction.redactTokens(body).contains(SECRET));
	}

	@Test
	void malformedTokensOnTokenRoutesAreHidden() {
		assertEquals("/registrationConfirmation/[hidden]", YadaLogRedaction.redactTokens("/registrationConfirmation/3-" + SECRET + "-1"));
		assertEquals("/autologin/[hidden]?action=%2F", YadaLogRedaction.redactTokens("/autologin/abc-" + SECRET.substring(3) + "?action=%2F"));
		assertEquals("/yadaPasswordChangeModal/[hidden]/user/end", YadaLogRedaction.redactTokens("/yadaPasswordChangeModal/3-123/user/end"));
	}

	@Test
	void ordinaryPathsAreUnchanged() {
		assertEquals("/passwordReset", YadaLogRedaction.redactTokens("/passwordReset"));
		assertEquals("/article/2024-10-01/view", YadaLogRedaction.redactTokens("/article/2024-10-01/view"));
		assertNull(YadaLogRedaction.redactTokens(null));
	}

	@Test
	void secretParametersAreHidden() {
		assertEquals("a=1&token=[hidden]&_csrf=[hidden]&password=[hidden]&b=2", YadaLogRedaction.redactQueryString("a=1&token=3-" + SECRET + "&_csrf=abc&password=x&b=2"));
		assertEquals("action=%2Fhome", YadaLogRedaction.redactQueryString("action=%2Fhome"));
		assertNull(YadaLogRedaction.redactQueryString(null));
	}
}
