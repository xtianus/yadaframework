package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
 * Registration and password recovery requests (YadaRegistrationController, YadaSecurityUtil, YadaRegistrationRequestDao)
 * over HTTP: only the submitted email and password reach a stored request, links work once, for their own purpose and
 * within their lifetime, a new request replaces the previous link, and the database holds no usable secret.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationTokenSecurityTest {
	private static final String NEW_USER_EMAIL = "newcomer@yts.test";
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String OTHER_PASSWORD = "another-new-password";
	private static final String PASSWORD_CHANGED = "Your password has been changed";
	private static final int TYPE_REGISTRATION = 0;
	private static final int TYPE_PASSWORD_RECOVERY = 1;

	private ApplicationSession session;
	private CapturingMailSender mailbox;
	private TestDataService testDataService;
	private AuthDb db;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
		mailbox = session.application().getBean(CapturingMailSender.class);
		testDataService = session.application().getBean(TestDataService.class);
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

	/**
	 * Requests a password recovery email and returns its link
	 */
	private String requestRecovery(String email) throws Exception {
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields("/passwordReset", "passwordResetForm");
		fields.put("email", email);
		HttpResponse<String> response = client.post("/yadaPasswordResetPost", fields);
		assertEquals(302, response.statusCode(), response.body());
		return mailbox.lastMessageTo(email).link();
	}

	/**
	 * Sends the registration form and returns the confirmation link
	 */
	private String register(String email, String password) throws Exception {
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields("/registerPage", "registerForm");
		fields.put("email", email);
		fields.put("password", password);
		HttpResponse<String> response = client.post("/signup", fields);
		assertEquals(302, response.statusCode(), response.body());
		return mailbox.lastMessageTo(email).link();
	}

	/**
	 * Posts a new password with a recovery token, as the password change form does
	 * @return the response body
	 */
	private String changePassword(AuthTestClient client, String token, String username, String password) throws Exception {
		Map<String, String> fields = Map.of("_csrf", client.csrfToken(), "token", token, "username", username, "password", password, "confirmPassword", password);
		HttpResponse<String> response = client.post("/passwordChangeAfterRequest", fields);
		return Jsoup.parse(response.body()).text();
	}

	private boolean canLogIn(String email, String password) throws Exception {
		AuthTestClient client = newClient();
		client.login(email, password);
		return ("Logged in as " + email).equals(client.loggedAs());
	}

	private Map<String, Object> requestRow(long id) throws Exception {
		List<Map<String, Object>> rows = db.rows("SELECT * FROM YadaRegistrationRequest WHERE id=?", id);
		return rows.isEmpty() ? null : rows.get(0);
	}

	@Test
	void theRecoveryRequestIgnoresForgedFields() throws Exception {
		String attackerLink = requestRecovery(TestDataService.USER_EMAIL);
		long attackerRequestId = AuthTestClient.idOf(attackerLink);
		AuthTestClient attacker = newClient();
		Map<String, String> fields = attacker.formFields("/passwordReset", "passwordResetForm");
		fields.put("email", TestDataService.ADMIN_EMAIL);
		fields.put("id", Long.toString(attackerRequestId));
		fields.put("token", "1234");
		fields.put("tokenHash", "0".repeat(64));
		fields.put("timestamp", "2000-01-01");
		fields.put("expiration", "2100-01-01");
		fields.put("registrationType", "REGISTRATION");
		fields.put("yadaUserCredentials.id", "1");
		fields.put("password", "attacker-password");
		HttpResponse<String> response = attacker.post("/yadaPasswordResetPost", fields);
		assertEquals(302, response.statusCode(), response.body());

		Map<String, Object> attackerRow = requestRow(attackerRequestId);
		assertNotNull(attackerRow, "The attacker request was replaced");
		assertEquals(TestDataService.USER_EMAIL, attackerRow.get("email"));
		List<Map<String, Object>> adminRows = db.rows("SELECT * FROM YadaRegistrationRequest WHERE email=?", TestDataService.ADMIN_EMAIL);
		assertEquals(1, adminRows.size());
		Map<String, Object> adminRow = adminRows.get(0);
		assertNotEquals(attackerRequestId, ((Number) adminRow.get("id")).longValue());
		assertEquals(TYPE_PASSWORD_RECOVERY, ((Number) adminRow.get("registrationType")).intValue());
		assertNull(adminRow.get("yadaUserCredentials_id"));
		assertNotEquals("attacker-password", adminRow.get("password"));
		// The attacker chosen secrets don't open the reset form
		assertEquals(302, newClient().get("/passwordReset/" + attackerRequestId + "-1234").statusCode());
		assertEquals(302, newClient().get("/passwordReset/" + adminRow.get("id") + "-" + "0".repeat(64)).statusCode());
	}

	@Test
	void aRecoveryLinkWorksOnlyOnce() throws Exception {
		String link = requestRecovery(TestDataService.USER_EMAIL);
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields(AuthTestClient.pathAndQuery(link), "passwordChangeForm");
		String token = fields.get("token");
		assertTrue(changePassword(client, token, TestDataService.USER_EMAIL, NEW_PASSWORD).contains(PASSWORD_CHANGED));

		AuthTestClient replay = newClient();
		assertEquals(302, replay.get(AuthTestClient.pathAndQuery(link)).statusCode(), "A used link opens the form");
		assertFalse(changePassword(replay, token, TestDataService.USER_EMAIL, OTHER_PASSWORD).contains(PASSWORD_CHANGED));
		assertTrue(canLogIn(TestDataService.USER_EMAIL, NEW_PASSWORD));
	}

	@Test
	void aNewRecoveryRequestInvalidatesThePreviousLink() throws Exception {
		String first = requestRecovery(TestDataService.USER_EMAIL);
		String second = requestRecovery(TestDataService.USER_EMAIL);
		assertNotEquals(first, second);
		assertEquals(302, newClient().get(AuthTestClient.pathAndQuery(first)).statusCode());
		AuthTestClient client = newClient();
		assertFalse(changePassword(client, AuthTestClient.tokenOf(first), TestDataService.USER_EMAIL, NEW_PASSWORD).contains(PASSWORD_CHANGED));
		assertEquals(200, newClient().get(AuthTestClient.pathAndQuery(second)).statusCode());
	}

	@Test
	void anExpiredRecoveryLinkIsRejected() throws Exception {
		String link = requestRecovery(TestDataService.USER_EMAIL);
		db.update("UPDATE YadaRegistrationRequest SET expiration=?", new Timestamp(System.currentTimeMillis() - 1000));
		assertEquals(302, newClient().get(AuthTestClient.pathAndQuery(link)).statusCode(), "An expired link opens the form");
		assertFalse(changePassword(newClient(), AuthTestClient.tokenOf(link), TestDataService.USER_EMAIL, NEW_PASSWORD).contains(PASSWORD_CHANGED));
		assertTrue(canLogIn(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD));
	}

	@Test
	void requestsGetTheDefaultLifetimes() throws Exception {
		requestRecovery(TestDataService.USER_EMAIL);
		register(NEW_USER_EMAIL, NEW_PASSWORD);
		long recoveryMinutes = db.count("SELECT TIMESTAMPDIFF(MINUTE, timestamp, expiration) FROM YadaRegistrationRequest WHERE registrationType=?", TYPE_PASSWORD_RECOVERY);
		long registrationMinutes = db.count("SELECT TIMESTAMPDIFF(MINUTE, timestamp, expiration) FROM YadaRegistrationRequest WHERE registrationType=?", TYPE_REGISTRATION);
		assertEquals(30, recoveryMinutes);
		assertEquals(24 * 60, registrationMinutes);
	}

	@Test
	void aRegistrationLinkCannotChangeAPassword() throws Exception {
		String link = register(NEW_USER_EMAIL, NEW_PASSWORD);
		// The account appears by other means while the registration link is still valid
		testDataService.createUser(NEW_USER_EMAIL, NEW_PASSWORD, "newcomer", "USER");
		assertEquals(302, newClient().get("/passwordReset/" + AuthTestClient.tokenOf(link)).statusCode());
		assertFalse(changePassword(newClient(), AuthTestClient.tokenOf(link), NEW_USER_EMAIL, OTHER_PASSWORD).contains(PASSWORD_CHANGED));
		assertTrue(canLogIn(NEW_USER_EMAIL, NEW_PASSWORD));
	}

	@Test
	void aRecoveryLinkCannotConfirmARegistration() throws Exception {
		String link = requestRecovery(TestDataService.USER_EMAIL);
		AuthTestClient client = newClient();
		HttpResponse<String> response = client.get("/registrationConfirmation/" + AuthTestClient.tokenOf(link));
		assertEquals(302, response.statusCode());
		assertTrue(client.homeText().contains("The registration link is no longer valid"), client.homeText());
		assertNotNull(requestRow(AuthTestClient.idOf(link)), "The recovery request was consumed by another flow");
	}

	@Test
	void theSignupIgnoresForgedFields() throws Exception {
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields("/registerPage", "registerForm");
		fields.put("email", NEW_USER_EMAIL);
		fields.put("password", NEW_PASSWORD);
		fields.put("id", "999");
		fields.put("token", "1234");
		fields.put("tokenHash", "0".repeat(64));
		fields.put("timestamp", "2000-01-01");
		fields.put("expiration", "2100-01-01");
		fields.put("registrationType", "PASSWORD_RECOVERY");
		fields.put("yadaUserCredentials.id", "1");
		HttpResponse<String> response = client.post("/signup", fields);
		assertEquals(302, response.statusCode(), response.body());

		List<Map<String, Object>> rows = db.rows("SELECT * FROM YadaRegistrationRequest");
		assertEquals(1, rows.size());
		Map<String, Object> row = rows.get(0);
		assertNotEquals(999L, ((Number) row.get("id")).longValue());
		assertEquals(TYPE_REGISTRATION, ((Number) row.get("registrationType")).intValue());
		assertNull(row.get("yadaUserCredentials_id"));
		assertNotEquals("0".repeat(64), row.get("tokenHash"));
		assertTrue(((Timestamp) row.get("timestamp")).getTime() > System.currentTimeMillis() - 3600_000L, "Forged timestamp stored");
	}

	@Test
	void aRegistrationLinkWorksOnlyOnce() throws Exception {
		String link = register(NEW_USER_EMAIL, NEW_PASSWORD);
		AuthTestClient client = newClient();
		assertEquals(302, client.get(AuthTestClient.pathAndQuery(link)).statusCode());
		assertNotNull(testDataService.findUser(NEW_USER_EMAIL));
		assertTrue(client.homeText().contains("Registration completed"));

		AuthTestClient replay = newClient();
		replay.get(AuthTestClient.pathAndQuery(link));
		assertTrue(replay.homeText().contains("The registration link is no longer valid"), replay.homeText());
		assertEquals(1, db.count("SELECT COUNT(*) FROM YadaUserCredentials WHERE username=?", NEW_USER_EMAIL));
	}

	@Test
	void anExpiredRegistrationLinkIsRejected() throws Exception {
		String link = register(NEW_USER_EMAIL, NEW_PASSWORD);
		db.update("UPDATE YadaRegistrationRequest SET expiration=?", new Timestamp(System.currentTimeMillis() - 1000));
		AuthTestClient client = newClient();
		client.get(AuthTestClient.pathAndQuery(link));
		assertNull(testDataService.findUser(NEW_USER_EMAIL));
		assertTrue(client.homeText().contains("The registration link is no longer valid"), client.homeText());
	}

	@Test
	void aForgedRegistrationSecretIsRejected() throws Exception {
		String link = register(NEW_USER_EMAIL, NEW_PASSWORD);
		String secret = AuthTestClient.secretOf(link);
		String digest = (String) db.rows("SELECT tokenHash FROM YadaRegistrationRequest").get(0).get("tokenHash");
		for (String forgedSecret : new String[] { digest, (secret.charAt(0) == 'a' ? 'b' : 'a') + secret.substring(1) }) {
			AuthTestClient client = newClient();
			client.get(URI.create(link).getRawPath().replace(secret, forgedSecret));
			assertNull(testDataService.findUser(NEW_USER_EMAIL));
		}
		assertEquals(1, db.count("SELECT COUNT(*) FROM YadaRegistrationRequest"));
	}

	@Test
	void theDatabaseHoldsNoRawSecret() throws Exception {
		String recovery = requestRecovery(TestDataService.USER_EMAIL);
		String registration = register(NEW_USER_EMAIL, NEW_PASSWORD);
		String table = db.tableText("YadaRegistrationRequest");
		assertFalse(table.contains(AuthTestClient.secretOf(recovery)));
		assertFalse(table.contains(AuthTestClient.secretOf(registration)));
	}

	@Test
	void anEmailChangeLinkWorksOnceAndOnlyForItsPurpose() throws Exception {
		String newEmail = "changed@yts.test";
		AuthTestClient user = newClient();
		user.login(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD);
		HttpResponse<String> request = user.post("/test/security/auth/emailChange", Map.of("_csrf", user.csrfToken(), "newEmail", newEmail));
		assertEquals(302, request.statusCode(), request.body());
		String link = mailbox.lastMessageTo(newEmail).link();
		assertTrue(AuthTestClient.pathAndQuery(link).startsWith("/changeEmailConfirm/"), link);
		assertEquals(30, db.count("SELECT TIMESTAMPDIFF(MINUTE, timestamp, expiration) FROM YadaRegistrationRequest"));

		// Other flows can't use it
		assertFalse(changePassword(newClient(), AuthTestClient.tokenOf(link), newEmail, NEW_PASSWORD).contains(PASSWORD_CHANGED));
		String recovery = requestRecovery(TestDataService.USER_EMAIL);
		AuthTestClient wrongPurpose = newClient();
		wrongPurpose.get("/changeEmailConfirm/" + AuthTestClient.tokenOf(recovery));
		assertTrue(wrongPurpose.homeText().contains("The email change link is no longer valid"), wrongPurpose.homeText());
		assertEquals(0, db.count("SELECT COUNT(*) FROM YadaUserCredentials WHERE username=?", newEmail));

		AuthTestClient confirm = newClient();
		confirm.get(AuthTestClient.pathAndQuery(link));
		assertTrue(confirm.homeText().contains("Your email has been changed"), confirm.homeText());
		assertEquals(1, db.count("SELECT COUNT(*) FROM YadaUserCredentials WHERE username=?", newEmail));

		AuthTestClient replay = newClient();
		replay.get(AuthTestClient.pathAndQuery(link));
		assertTrue(replay.homeText().contains("The email change link is no longer valid"), replay.homeText());
	}

	@Test
	void concurrentPasswordChangesWithOneLinkSucceedOnce() throws Exception {
		String link = requestRecovery(TestDataService.USER_EMAIL);
		String token = AuthTestClient.tokenOf(link);
		int attempts = 6;
		List<AuthTestClient> clients = new ArrayList<>();
		List<String> csrfTokens = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			AuthTestClient client = newClient();
			clients.add(client);
			csrfTokens.add(client.csrfToken());
		}
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		try {
			List<Future<String>> results = new ArrayList<>();
			for (int i = 0; i < attempts; i++) {
				AuthTestClient client = clients.get(i);
				Map<String, String> fields = Map.of("_csrf", csrfTokens.get(i), "token", token, "username", TestDataService.USER_EMAIL, "password", "password-" + i, "confirmPassword", "password-" + i);
				Callable<String> attempt = () -> {
					start.await();
					return Jsoup.parse(client.post("/passwordChangeAfterRequest", fields).body()).text();
				};
				results.add(executor.submit(attempt));
			}
			start.countDown();
			int successes = 0;
			for (Future<String> result : results) {
				if (result.get().contains(PASSWORD_CHANGED)) {
					successes++;
				}
			}
			assertEquals(1, successes);
		} finally {
			executor.shutdownNow();
		}
	}
}
