package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;

/**
 * Autologin links (YadaLoginController, YadaTokenHandler, YadaAutoLoginTokenDao) through the real Spring Security
 * filter chain: the GET of a link only shows a confirmation page, the login happens once with an explicit POST that
 * carries the CSRF token of the visitor session, every redirect stays on the site, and expired, disabled or forged
 * tokens never log in.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutologinSecurityTest {
	private static final String TARGET = "/test/user/area";
	private static final String CONFIRM_FORM = "autologinConfirmForm";

	private ApplicationSession session;
	private TestDataService testDataService;
	private AuthDb db;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
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

	private String issueUserLink(String action) {
		return testDataService.issueAutologinLink(TestDataService.USER_EMAIL, action);
	}

	private long tokenRows() throws Exception {
		return db.count("SELECT COUNT(*) FROM YadaAutoLoginToken");
	}

	private static String linkPath(String link) {
		return URI.create(link).getRawPath();
	}

	/**
	 * Opens the confirmation page of a link and posts its form as a user would
	 */
	private HttpResponse<String> confirm(AuthTestClient client, String pathAndQuery) throws Exception {
		Map<String, String> fields = client.formFields(pathAndQuery, CONFIRM_FORM);
		return client.post(URI.create(pathAndQuery).getRawPath(), fields);
	}

	/**
	 * Fields of a confirmation POST built without the confirmation page: the CSRF token of the client session and an action
	 */
	private static Map<String, String> postFields(AuthTestClient client, String action) throws Exception {
		Map<String, String> fields = new HashMap<>();
		fields.put("_csrf", client.csrfToken());
		fields.put("action", action);
		return fields;
	}

	private static void assertLocalLocation(HttpResponse<String> response) {
		assertEquals(302, response.statusCode(), response.body());
		String location = AuthTestClient.location(response);
		assertNotNull(location);
		assertTrue(location.startsWith("/") && !location.startsWith("//") && !location.startsWith("/\\"), "Redirect leaves the site: " + location);
		assertFalse(location.contains("evil"), "Redirect leaves the site: " + location);
	}

	@Test
	void getShowsTheConfirmationPageWithoutLoggingInOrConsuming() throws Exception {
		String link = issueUserLink(TARGET);
		AuthTestClient client = newClient();
		HttpResponse<String> page = client.get(AuthTestClient.pathAndQuery(link));
		assertEquals(200, page.statusCode(), page.body());
		Document document = Jsoup.parse(page.body());
		assertNotNull(document.selectFirst("form[data-testid=" + CONFIRM_FORM + "][method=post]"), "No confirmation form");
		assertNotNull(document.selectFirst("form[data-testid=" + CONFIRM_FORM + "] input[name=_csrf]"), "No CSRF token in the confirmation form");
		assertEquals(TestDataService.USER_EMAIL, document.selectFirst("[data-testid=autologinAccount]").text(), "The account is not shown");
		assertFalse(page.body().contains(".submit()"), "The confirmation page must not submit by itself");
		assertNull(client.loggedAs());
		assertEquals(1, tokenRows());
	}

	@Test
	void theConfirmationPostLogsInOnlyOnce() throws Exception {
		String link = issueUserLink(TARGET);
		AuthTestClient client = newClient();
		HttpResponse<String> response = confirm(client, AuthTestClient.pathAndQuery(link));
		assertEquals(302, response.statusCode(), response.body());
		assertEquals(TARGET, AuthTestClient.location(response));
		assertEquals("Logged in as " + TestDataService.USER_EMAIL, client.loggedAs());
		assertEquals(0, tokenRows());

		AuthTestClient second = newClient();
		assertNull(second.formFieldsOrNull(AuthTestClient.pathAndQuery(link), CONFIRM_FORM), "A used link still offers the confirmation");
		HttpResponse<String> replay = second.post(linkPath(link), postFields(second, TARGET));
		assertLocalLocation(replay);
		assertNull(second.loggedAs(), "A used link logged in again");
	}

	@Test
	void aPostWithoutCsrfTokenIsRejected() throws Exception {
		String link = issueUserLink(TARGET);
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields(AuthTestClient.pathAndQuery(link), CONFIRM_FORM);
		fields.remove("_csrf");
		HttpResponse<String> response = client.post(linkPath(link), fields);
		assertEquals(403, response.statusCode());
		assertNull(client.loggedAs());
		assertEquals(1, tokenRows());
	}

	@Test
	void aForeignSitePostIsRejected() throws Exception {
		String link = issueUserLink(TARGET);
		// A cross-site form post: the victim browser has a session but the attacker can't read its CSRF token
		AuthTestClient victim = newClient();
		victim.get("/test/home");
		Map<String, String> fields = new HashMap<>();
		fields.put("action", TARGET);
		HttpResponse<String> response = victim.post(linkPath(link), fields);
		assertEquals(403, response.statusCode());
		assertNull(victim.loggedAs());
		assertEquals(1, tokenRows());
	}

	@Test
	void theCsrfTokenOfAnotherSessionIsRejected() throws Exception {
		String link = issueUserLink(TARGET);
		AuthTestClient attacker = newClient();
		Map<String, String> attackerFields = attacker.formFields(AuthTestClient.pathAndQuery(link), CONFIRM_FORM);
		AuthTestClient victim = newClient();
		victim.get("/test/home");
		HttpResponse<String> response = victim.post(linkPath(link), attackerFields);
		assertEquals(403, response.statusCode());
		assertNull(victim.loggedAs());
		assertEquals(1, tokenRows());
	}

	@ParameterizedTest
	@ValueSource(strings = { "https://evil.example/", "//evil.example/", "/\\evil.example/", "\\\\evil.example", "%2F%2Fevil.example", "/%2F%2Fevil.example", "/%252F%252Fevil.example", "https:%2F%2Fevil.example", "/ok\r\nLocation: https://evil.example", " //evil.example" })
	void unsafeActionsNeverRedirectOutside(String unsafeAction) throws Exception {
		String link = issueUserLink(TARGET);
		String tampered = linkPath(link) + "?action=" + URLEncoder.encode(unsafeAction, StandardCharsets.UTF_8);
		AuthTestClient client = newClient();
		HttpResponse<String> page = client.get(tampered);
		if (page.statusCode() == 302) {
			assertLocalLocation(page);
		}
		Map<String, String> fields = client.formFieldsOrNull(tampered, CONFIRM_FORM);
		if (fields == null) {
			fields = postFields(client, unsafeAction);
		}
		// The hidden field could also be changed by hand
		fields.put("action", unsafeAction);
		HttpResponse<String> response = client.post(linkPath(link), fields);
		assertLocalLocation(response);
	}

	@Test
	void aLocalActionWithQueryStringIsKept() throws Exception {
		String target = TARGET + "?tab=users&page=2";
		String link = issueUserLink(target);
		AuthTestClient client = newClient();
		HttpResponse<String> response = confirm(client, AuthTestClient.pathAndQuery(link));
		assertEquals(302, response.statusCode(), response.body());
		assertEquals(target, AuthTestClient.location(response));
	}

	@Test
	void anotherUsersSessionIsNeverReplaced() throws Exception {
		String link = issueUserLink(TARGET);
		AuthTestClient admin = newClient();
		admin.login(TestDataService.ADMIN_EMAIL, TestDataService.ADMIN_PASSWORD);
		assertEquals("Logged in as " + TestDataService.ADMIN_EMAIL, admin.loggedAs());

		HttpResponse<String> page = admin.get(AuthTestClient.pathAndQuery(link));
		if (page.statusCode() == 302) {
			assertLocalLocation(page);
		}
		assertEquals("Logged in as " + TestDataService.ADMIN_EMAIL, admin.loggedAs());

		HttpResponse<String> response = admin.post(linkPath(link), postFields(admin, TARGET));
		assertLocalLocation(response);
		assertEquals("Logged in as " + TestDataService.ADMIN_EMAIL, admin.loggedAs());
		assertEquals(1, tokenRows(), "The token was consumed without logging in");
	}

	@Test
	void aSessionWithoutUserProfileIsNeverReplaced() throws Exception {
		// A user with credentials but no YadaUserProfile, with the password of the seeded user
		String bareEmail = "bare@yts.test";
		db.update("INSERT INTO YadaUserCredentials (changePassword, enabled, failedAttempts, version, password, username) SELECT 0, 1, 0, 0, password, ? FROM YadaUserCredentials WHERE username=?", bareEmail, TestDataService.USER_EMAIL);
		db.update("INSERT INTO YadaUserCredentials_roles (roles, YadaUserCredentials_id) SELECT r.roles, (SELECT id FROM YadaUserCredentials WHERE username=?) FROM YadaUserCredentials_roles r JOIN YadaUserCredentials c ON c.id=r.YadaUserCredentials_id WHERE c.username=?", bareEmail, TestDataService.USER_EMAIL);
		String link = issueUserLink(TARGET);
		AuthTestClient bare = newClient();
		bare.login(bareEmail, TestDataService.USER_PASSWORD);
		assertEquals("Logged in as " + bareEmail, bare.loggedAs());

		HttpResponse<String> page = bare.get(AuthTestClient.pathAndQuery(link));
		assertNull(Jsoup.parse(page.body()).selectFirst("[data-testid=autologinAccount]"), "The confirmation is offered to a logged-in user");
		HttpResponse<String> response = bare.post(linkPath(link), postFields(bare, TARGET));
		assertLocalLocation(response);
		assertEquals("Logged in as " + bareEmail, bare.loggedAs());
		assertEquals(1, tokenRows(), "The token was consumed without logging in");
	}

	@Test
	void theCsrfTokenIsRenewedByTheAutologin() throws Exception {
		String link = issueUserLink(TARGET);
		AuthTestClient client = newClient();
		Map<String, String> fields = client.formFields(AuthTestClient.pathAndQuery(link), CONFIRM_FORM);
		String preLoginToken = fields.get("_csrf");
		assertEquals(302, client.post(linkPath(link), fields).statusCode());
		assertEquals("Logged in as " + TestDataService.USER_EMAIL, client.loggedAs());
		assertEquals(403, client.post("/test/echo", Map.of("_csrf", preLoginToken, "message", "hello")).statusCode(), "The pre-login CSRF token is still valid");
		HttpResponse<String> echo = client.post("/test/echo", Map.of("_csrf", client.csrfToken(), "message", "hello"));
		assertEquals(200, echo.statusCode(), echo.body());
	}

	@Test
	void theSameUserIsJustRedirected() throws Exception {
		String link = issueUserLink(TARGET);
		AuthTestClient user = newClient();
		user.login(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD);
		HttpResponse<String> response = user.post(linkPath(link), postFields(user, TARGET));
		assertEquals(302, response.statusCode());
		assertEquals(TARGET, AuthTestClient.location(response));
		assertEquals("Logged in as " + TestDataService.USER_EMAIL, user.loggedAs());
	}

	@Test
	void aTokenWithoutExpirationIsRejected() throws Exception {
		String link = issueUserLink(TARGET);
		db.update("UPDATE YadaAutoLoginToken SET expiration=NULL");
		assertRejected(link);
	}

	@Test
	void anExpiredTokenIsRejected() throws Exception {
		String link = issueUserLink(TARGET);
		db.update("UPDATE YadaAutoLoginToken SET expiration=?", new Timestamp(System.currentTimeMillis() - 1000));
		assertRejected(link);
	}

	@Test
	void aDisabledUserCannotLogIn() throws Exception {
		String link = issueUserLink(TARGET);
		db.update("UPDATE YadaUserCredentials SET enabled=0 WHERE username=?", TestDataService.USER_EMAIL);
		assertRejected(link);
		assertEquals(1, tokenRows(), "A token of a disabled user must not be consumed");
	}

	@Test
	void aForgedSecretIsRejected() throws Exception {
		String link = issueUserLink(TARGET);
		String secret = AuthTestClient.secretOf(link);
		String forgedSecret = (secret.charAt(0) == 'a' ? 'b' : 'a') + secret.substring(1);
		String forged = link.replace(secret, forgedSecret);
		assertRejected(forged);
		assertEquals(1, tokenRows());
	}

	@Test
	void theStoredDigestIsNotASecret() throws Exception {
		String link = issueUserLink(TARGET);
		String digest = (String) db.rows("SELECT tokenHash FROM YadaAutoLoginToken").get(0).get("tokenHash");
		String forged = link.replace(AuthTestClient.secretOf(link), digest);
		assertRejected(forged);
		assertEquals(1, tokenRows());
	}

	@Test
	void malformedLinksAreRejected() throws Exception {
		String link = issueUserLink(TARGET);
		long id = AuthTestClient.idOf(link);
		String secret = AuthTestClient.secretOf(link);
		for (String token : new String[] { "abc-" + secret, id + "-" + secret + "-1", id + "-" + secret.substring(1), id + "-" + secret + "0", "-" + id + "-" + secret, (id + 1000) + "-" + secret, id + "-" + secret.toUpperCase(), "99999999999999999999999-" + secret }) {
			assertRejected("http://localhost/autologin/" + token + "?action=" + TARGET);
		}
		assertEquals(1, tokenRows());
	}

	@Test
	void theDatabaseHoldsNoRawSecret() throws Exception {
		String link = issueUserLink(TARGET);
		assertFalse(db.tableText("YadaAutoLoginToken").contains(AuthTestClient.secretOf(link)));
	}

	/**
	 * Neither the confirmation page nor a POST with a valid CSRF token log in with the link
	 */
	private void assertRejected(String link) throws Exception {
		AuthTestClient client = newClient();
		String pathAndQuery = AuthTestClient.pathAndQuery(link);
		HttpResponse<String> page = client.get(pathAndQuery);
		assertTrue(page.statusCode() == 302 || page.statusCode() == 200, "Status " + page.statusCode());
		if (page.statusCode() == 302) {
			assertLocalLocation(page);
		}
		assertNull(Jsoup.parse(page.body()).selectFirst("[data-testid=autologinAccount]"), "An invalid link shows an account");
		assertNull(client.loggedAs());
		HttpResponse<String> response = client.post(linkPath(link), postFields(client, TARGET));
		assertTrue(response.statusCode() == 302 || response.statusCode() == 400 || response.statusCode() == 404, "Status " + response.statusCode());
		assertNull(client.loggedAs(), "Logged in with " + link);
	}
}
