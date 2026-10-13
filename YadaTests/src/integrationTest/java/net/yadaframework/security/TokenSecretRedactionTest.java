package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.tests.components.CapturingMailSender;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;
import net.yadaframework.tests.support.LogCapture;

/**
 * Bearer secrets of autologin, registration and password recovery links never reach the logs: not through the
 * AuditFilter request lines, the controllers, the token parser, the email service or the toString() of the entities,
 * whatever the outcome (success, expired, malformed, unknown, repeated) and during the cleanup of old requests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenSecretRedactionTest {
	private static final String NEW_USER_EMAIL = "newcomer@yts.test";
	private static final String NEW_PASSWORD = "brand-new-password";
	private static final String TARGET = "/test/user/area";

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
	 * The captured lines, except the DEBUG and TRACE lines of Spring and Hibernate: those libraries log full request
	 * urls and entity states at debug level, which production configurations must not enable (a deployment condition)
	 */
	private static String applicationLogs(LogCapture capture) {
		StringBuilder text = new StringBuilder();
		for (ILoggingEvent event : capture.events()) {
			boolean library = event.getLoggerName().startsWith("org.springframework") || event.getLoggerName().startsWith("org.hibernate");
			if (library && !event.getLevel().isGreaterOrEqual(Level.INFO)) {
				continue;
			}
			text.append(event.getLevel()).append(' ').append(event.getLoggerName()).append(" - ").append(event.getFormattedMessage());
			if (event.getThrowableProxy() != null) {
				text.append('\n').append(ThrowableProxyUtil.asString(event.getThrowableProxy()));
			}
			text.append('\n');
		}
		return text.toString();
	}

	private static void assertNoSecret(String logs, List<String> links) {
		assertTrue(logs.length() > 0, "Nothing captured");
		for (String link : links) {
			String secret = AuthTestClient.secretOf(link);
			assertFalse(logs.contains(secret), "Secret of " + AuthTestClient.idOf(link) + " logged:\n" + logs);
			assertFalse(logs.contains(AuthTestClient.tokenOf(link)), "Token link logged:\n" + logs);
		}
	}

	private void autologinRoundTrip(AuthTestClient client, String link) throws Exception {
		String pathAndQuery = AuthTestClient.pathAndQuery(link);
		Map<String, String> fields = client.formFieldsOrNull(pathAndQuery, "autologinConfirmForm");
		if (fields == null) {
			fields = new HashMap<>();
			fields.put("_csrf", client.csrfToken());
			fields.put("action", TARGET);
		}
		client.post(URI.create(link).getRawPath(), fields);
	}

	@Test
	void autologinSecretsAreNotLogged() throws Exception {
		List<String> links = new ArrayList<>();
		try (LogCapture logs = LogCapture.start()) {
			String used = testDataService.issueAutologinLink(TestDataService.USER_EMAIL, TARGET);
			String expired = testDataService.issueAutologinLink(TestDataService.ADMIN_EMAIL, TARGET);
			links.add(used);
			links.add(expired);
			db.update("UPDATE YadaAutoLoginToken SET expiration=? WHERE id=?", new Timestamp(System.currentTimeMillis() - 1000), AuthTestClient.idOf(expired));
			autologinRoundTrip(newClient(), used); // Success
			autologinRoundTrip(newClient(), used); // Repeated
			autologinRoundTrip(newClient(), expired); // Expired
			String secret = AuthTestClient.secretOf(used);
			String unknown = used.replace(AuthTestClient.idOf(used) + "-", (AuthTestClient.idOf(used) + 100) + "-");
			autologinRoundTrip(newClient(), unknown); // Unknown id
			String malformed = used.replace(secret, secret + "-x");
			autologinRoundTrip(newClient(), malformed); // Malformed
			// A new token cleans up the expired ones
			links.add(testDataService.issueAutologinLink(TestDataService.USER_EMAIL, TARGET));
			assertNoSecret(applicationLogs(logs), links);
			assertFalse(applicationLogs(logs).contains(secret + "-x"));
		}
	}

	@Test
	void registrationAndRecoverySecretsAreNotLogged() throws Exception {
		List<String> links = new ArrayList<>();
		try (LogCapture logs = LogCapture.start()) {
			// Registration, confirmed twice
			AuthTestClient client = newClient();
			Map<String, String> fields = client.formFields("/registerPage", "registerForm");
			fields.put("email", NEW_USER_EMAIL);
			fields.put("password", NEW_PASSWORD);
			client.post("/signup", fields);
			String registration = mailbox.lastMessageTo(NEW_USER_EMAIL).link();
			links.add(registration);
			newClient().get(AuthTestClient.pathAndQuery(registration));
			newClient().get(AuthTestClient.pathAndQuery(registration));

			// Two recovery requests: the second replaces the first
			for (int i = 0; i < 2; i++) {
				AuthTestClient resetClient = newClient();
				Map<String, String> resetFields = resetClient.formFields("/passwordReset", "passwordResetForm");
				resetFields.put("email", TestDataService.USER_EMAIL);
				resetClient.post("/yadaPasswordResetPost", resetFields);
				links.add(mailbox.lastMessageTo(TestDataService.USER_EMAIL).link());
			}
			String replaced = links.get(1);
			String recovery = links.get(2);
			newClient().get(AuthTestClient.pathAndQuery(replaced));
			AuthTestClient changeClient = newClient();
			Map<String, String> changeFields = changeClient.formFields(AuthTestClient.pathAndQuery(recovery), "passwordChangeForm");
			changeFields.put("password", NEW_PASSWORD);
			changeFields.put("confirmPassword", NEW_PASSWORD);
			changeClient.post("/passwordChangeAfterRequest", changeFields);
			// Repeated
			AuthTestClient replayClient = newClient();
			changeFields.put("_csrf", replayClient.csrfToken());
			replayClient.post("/passwordChangeAfterRequest", changeFields);
			newClient().get(AuthTestClient.pathAndQuery(recovery));
			newClient().get("/registrationConfirmation/" + AuthTestClient.tokenOf(recovery) + "-1");
			assertNoSecret(applicationLogs(logs), links);
		}
	}
}
