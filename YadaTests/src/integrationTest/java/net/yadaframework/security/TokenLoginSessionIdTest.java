package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.Map;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.tests.components.CapturingMailSender;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;

/**
 * The password change after a recovery request and the registration confirmation log the user in: the session id
 * and the CSRF token change at that login, so that a session id or token known before (session fixation) never becomes
 * part of an authenticated session.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenLoginSessionIdTest {
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String NEW_USER_EMAIL = "fixation.newuser@example.com";

	private ApplicationSession session;
	private CapturingMailSender mailbox;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
		mailbox = session.application().getBean(CapturingMailSender.class);
	}

	@AfterAll
	void stopApplication() {
		if (session != null) {
			session.close();
		}
	}

	@BeforeEach
	void resetData() {
		session.fixtures().reset();
	}

	private AuthTestClient newClient() {
		return new AuthTestClient(session.application());
	}

	@Test
	void thePasswordChangeLoginChangesTheSessionId() throws Exception {
		AuthTestClient requester = newClient();
		Map<String, String> resetFields = requester.formFields("/passwordReset", "passwordResetForm");
		resetFields.put("email", TestDataService.USER_EMAIL);
		assertEquals(302, requester.post("/yadaPasswordResetPost", resetFields).statusCode());
		String link = mailbox.lastMessageTo(TestDataService.USER_EMAIL).link();

		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields(AuthTestClient.pathAndQuery(link), "passwordChangeForm");
		String sessionIdBefore = client.sessionId();
		assertNotNull(sessionIdBefore, "The password change form opened no session");
		fields.put("password", NEW_PASSWORD);
		fields.put("confirmPassword", NEW_PASSWORD);
		HttpResponse<String> change = client.post("/passwordChangeAfterRequest", fields);

		assertTrue(Jsoup.parse(change.body()).text().contains("Your password has been changed"), change.body());
		assertEquals("Logged in as " + TestDataService.USER_EMAIL, client.loggedAs());
		assertNotEquals(sessionIdBefore, client.sessionId(), "The session id was not changed at the password change login");
		// The token of the form is rejected after the login, the token of the new pages is accepted
		assertEquals(403, client.post("/yadaPasswordResetPost", Map.of("_csrf", fields.get("_csrf"), "email", TestDataService.USER_EMAIL)).statusCode(), "The CSRF token was not renewed at the password change login");
		assertEquals(302, client.post("/yadaPasswordResetPost", Map.of("_csrf", client.csrfToken(), "email", TestDataService.USER_EMAIL)).statusCode());
	}

	@Test
	void theRegistrationConfirmationLoginChangesTheSessionId() throws Exception {
		AuthTestClient registrant = newClient();
		Map<String, String> signupFields = registrant.formFields("/registerPage", "registerForm");
		signupFields.put("email", NEW_USER_EMAIL);
		signupFields.put("password", NEW_PASSWORD);
		assertEquals(302, registrant.post("/signup", signupFields).statusCode());
		String link = mailbox.lastMessageTo(NEW_USER_EMAIL).link();

		AuthTestClient client = newClient();
		client.csrfToken(); // Opens a session, as any page with a form does
		String sessionIdBefore = client.sessionId();
		assertNotNull(sessionIdBefore, "No session before the confirmation");

		assertEquals(302, client.get(AuthTestClient.pathAndQuery(link)).statusCode());

		assertEquals("Logged in as " + NEW_USER_EMAIL, client.loggedAs());
		assertNotEquals(sessionIdBefore, client.sessionId(), "The session id was not changed at the registration confirmation login");
	}
}
