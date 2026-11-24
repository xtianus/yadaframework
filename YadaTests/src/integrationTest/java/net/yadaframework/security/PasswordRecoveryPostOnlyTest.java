package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
 * The password recovery request ("/yadaPasswordResetPost") and the final password change ("/passwordChangeAfterRequest")
 * of YadaRegistrationController change state, so they answer only POST, where Spring Security checks the CSRF token.
 * A GET (a link, an image, a prefetch) creates no request, removes no pending request, sends no mail and changes no
 * password.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordRecoveryPostOnlyTest {
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String PASSWORD_CHANGED = "Your password has been changed";

	private ApplicationSession session;
	private CapturingMailSender mailbox;
	private AuthDb db;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
		mailbox = session.application().getBean(CapturingMailSender.class);
		db = new AuthDb(session.application());
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

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	/**
	 * Requests a password recovery email with the form, as a browser does, and returns its link
	 */
	private String requestRecovery(String email) throws Exception {
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields("/passwordReset", "passwordResetForm");
		fields.put("email", email);
		HttpResponse<String> response = client.post("/yadaPasswordResetPost", fields);
		assertEquals(302, response.statusCode(), response.body());
		return mailbox.lastMessageTo(email).link();
	}

	private List<Map<String, Object>> recoveryRows() throws Exception {
		return db.rows("SELECT id FROM YadaRegistrationRequest ORDER BY id");
	}

	private boolean canLogIn(String email, String password) throws Exception {
		AuthTestClient client = newClient();
		client.login(email, password);
		return ("Logged in as " + email).equals(client.loggedAs());
	}

	@Test
	void getDoesNotRequestARecovery() throws Exception {
		String pending = requestRecovery(TestDataService.USER_EMAIL);
		List<Map<String, Object>> rowsBefore = recoveryRows();
		int mailsBefore = mailbox.messagesTo(TestDataService.USER_EMAIL).size();

		HttpResponse<String> response = newClient().get("/yadaPasswordResetPost?email=" + encode(TestDataService.USER_EMAIL));

		assertEquals(405, response.statusCode());
		assertEquals(mailsBefore, mailbox.messagesTo(TestDataService.USER_EMAIL).size(), "A GET sent a recovery mail");
		assertEquals(rowsBefore, recoveryRows(), "A GET created or cleaned up recovery requests");
		// The pending link still works
		assertTrue(newClient().formFieldsOrNull(AuthTestClient.pathAndQuery(pending), "passwordChangeForm") != null, "The pending link was cleaned up");
	}

	@Test
	void postStillRequestsARecovery() throws Exception {
		String first = requestRecovery(TestDataService.USER_EMAIL);
		String second = requestRecovery(TestDataService.USER_EMAIL);
		assertFalse(first.equals(second));
		assertEquals(1, recoveryRows().size(), "The new request replaces the previous one");
	}

	@Test
	void getDoesNotChangeThePassword() throws Exception {
		String link = requestRecovery(TestDataService.USER_EMAIL);
		String token = AuthTestClient.tokenOf(link);
		String query = "token=" + encode(token) + "&username=" + encode(TestDataService.USER_EMAIL) + "&password=" + encode(NEW_PASSWORD) + "&confirmPassword=" + encode(NEW_PASSWORD);

		HttpResponse<String> response = newClient().get("/passwordChangeAfterRequest?" + query);

		assertEquals(405, response.statusCode());
		assertFalse(Jsoup.parse(response.body()).text().contains(PASSWORD_CHANGED));
		assertTrue(canLogIn(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD), "A GET changed the password");
		assertFalse(canLogIn(TestDataService.USER_EMAIL, NEW_PASSWORD));

		// The link was not used: the POST of the form still changes the password
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields(AuthTestClient.pathAndQuery(link), "passwordChangeForm");
		fields.put("password", NEW_PASSWORD);
		fields.put("confirmPassword", NEW_PASSWORD);
		HttpResponse<String> change = client.post("/passwordChangeAfterRequest", fields);
		assertTrue(Jsoup.parse(change.body()).text().contains(PASSWORD_CHANGED), change.body());
		assertTrue(canLogIn(TestDataService.USER_EMAIL, NEW_PASSWORD));
	}
}
