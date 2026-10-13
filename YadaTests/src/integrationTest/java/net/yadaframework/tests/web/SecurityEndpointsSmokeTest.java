package net.yadaframework.tests.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.tests.components.CapturedMail;
import net.yadaframework.tests.components.CapturingMailSender;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;
import net.yadaframework.tests.support.LogCapture;

/**
 * The security entry points of the test application are reachable over HTTP through the real Spring Security filter
 * chain: login, registration and password recovery with the links captured from the emails, and autologin. The per-run
 * configuration hook reaches the application. This is the base that the security regression tests build on.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityEndpointsSmokeTest {
	private static final String NEW_USER_EMAIL = "newcomer@yts.test";
	private static final String NEW_PASSWORD = "brand-new-password";

	private ApplicationSession session;
	private CapturingMailSender mailbox;
	private TestDataService testDataService;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(run -> run.setConfigurationValue("security/autologinExpirationHours", "5"), application -> application.getBean(TestDataService.class)::seedDefaults);
		mailbox = session.application().getBean(CapturingMailSender.class);
		testDataService = session.application().getBean(TestDataService.class);
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

	private static HttpClient newClient() {
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		return HttpClient.newBuilder().cookieHandler(cookieManager).followRedirects(HttpClient.Redirect.NEVER).build();
	}

	private HttpResponse<String> get(HttpClient client, String pathAndQuery) throws Exception {
		return client.send(HttpRequest.newBuilder(session.application().resolve(pathAndQuery)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private HttpResponse<String> post(HttpClient client, String path, Map<String, String> fields) throws Exception {
		String form = fields.entrySet().stream().map(field -> URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
		HttpRequest request = HttpRequest.newBuilder(session.application().resolve(path)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build();
		return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	/**
	 * Loads a page and returns the fields of one of its forms, including the CSRF token added by Spring Security
	 */
	private Map<String, String> formFields(HttpClient client, String pagePath, String formTestId) throws Exception {
		HttpResponse<String> page = get(client, pagePath);
		assertEquals(200, page.statusCode(), page.body());
		Element form = Jsoup.parse(page.body()).selectFirst("form[data-testid=" + formTestId + "]");
		assertNotNull(form, "No form " + formTestId + " in " + pagePath);
		Map<String, String> fields = new LinkedHashMap<>();
		for (Element input : form.select("input[name]")) {
			fields.put(input.attr("name"), input.attr("value"));
		}
		assertFalse(fields.getOrDefault("_csrf", "").isBlank(), "The form has no CSRF token");
		return fields;
	}

	private static String location(HttpResponse<String> response) {
		return response.headers().firstValue("Location").orElse(null);
	}

	/**
	 * @return the path and query of an absolute link found in an email
	 */
	private static String pathAndQuery(String link) {
		URI uri = URI.create(link);
		return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
	}

	private String loggedAs(HttpClient client) throws Exception {
		Document home = Jsoup.parse(get(client, "/test/home").body());
		Element loggedAs = home.selectFirst("[data-testid=loggedAs]");
		return loggedAs == null ? null : loggedAs.text();
	}

	private HttpResponse<String> login(HttpClient client, String email, String password) throws Exception {
		Map<String, String> fields = formFields(client, "/login", "loginForm");
		fields.put("username", email);
		fields.put("password", password);
		return post(client, "/loginPost", fields);
	}

	@Test
	void perRunSettingReachesTheApplication() {
		assertEquals(5, session.application().getBean(YadaConfiguration.class).getAutologinExpirationHours());
	}

	@Test
	void loginThroughTheForm() throws Exception {
		HttpClient client = newClient();
		HttpResponse<String> response = login(client, TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD);
		assertEquals(302, response.statusCode());
		assertEquals("Logged in as " + TestDataService.USER_EMAIL, loggedAs(client));
		assertEquals(200, get(client, "/test/user/area").statusCode());
	}

	@Test
	void registrationWithTheEmailedLink() throws Exception {
		HttpClient client = newClient();
		Map<String, String> fields = formFields(client, "/registerPage", "registerForm");
		fields.put("email", NEW_USER_EMAIL);
		fields.put("password", NEW_PASSWORD);
		HttpResponse<String> signup;
		try (LogCapture logs = LogCapture.start()) {
			signup = post(client, "/signup", fields);
			assertTrue(logs.text().contains("Sending email to=[" + NEW_USER_EMAIL + "]"), logs.text());
		}
		assertEquals(302, signup.statusCode(), signup.body());
		assertEquals("/test/home", URI.create(location(signup)).getPath());
		assertNull(testDataService.findUser(NEW_USER_EMAIL));

		CapturedMail mail = mailbox.lastMessageTo(NEW_USER_EMAIL);
		assertTrue(mail.subject().contains("YadaTests registration"), mail.subject());
		assertTrue(mail.text().contains(NEW_USER_EMAIL));
		String confirmationPath = pathAndQuery(mail.link());
		assertTrue(confirmationPath.startsWith("/registrationConfirmation/"), confirmationPath);

		HttpResponse<String> confirmation = get(client, confirmationPath);
		assertEquals(302, confirmation.statusCode(), confirmation.body());
		assertNotNull(testDataService.findUser(NEW_USER_EMAIL), "The registration was not completed");
		HttpResponse<String> outcome = get(client, URI.create(location(confirmation)).getPath());
		assertTrue(Jsoup.parse(outcome.body()).text().contains("Registration completed"), "The flash notification is lost");
	}

	@Test
	void passwordRecoveryWithTheEmailedLink() throws Exception {
		HttpClient client = newClient();
		Map<String, String> fields = formFields(client, "/passwordReset", "passwordResetForm");
		fields.put("email", TestDataService.USER_EMAIL);
		HttpResponse<String> request = post(client, "/yadaPasswordResetPost", fields);
		assertEquals(302, request.statusCode(), request.body());

		CapturedMail mail = mailbox.lastMessageTo(TestDataService.USER_EMAIL);
		String resetPath = pathAndQuery(mail.link());
		assertTrue(resetPath.startsWith("/passwordReset/"), resetPath);

		Map<String, String> changeFields = formFields(client, resetPath, "passwordChangeForm");
		assertEquals(TestDataService.USER_EMAIL, changeFields.get("username"));
		changeFields.put("password", NEW_PASSWORD);
		changeFields.put("confirmPassword", NEW_PASSWORD);
		HttpResponse<String> change = post(client, "/passwordChangeAfterRequest", changeFields);
		assertEquals(200, change.statusCode(), change.body());
		assertTrue(Jsoup.parse(change.body()).text().contains("Your password has been changed"), change.body());

		HttpClient otherClient = newClient();
		login(otherClient, TestDataService.USER_EMAIL, NEW_PASSWORD);
		assertEquals("Logged in as " + TestDataService.USER_EMAIL, loggedAs(otherClient));
	}

	@Test
	void autologinWithAnIssuedLink() throws Exception {
		String link = testDataService.issueAutologinLink(TestDataService.USER_EMAIL, "/test/user/area");
		HttpClient client = newClient();
		HttpResponse<String> autologin = get(client, pathAndQuery(link));
		assertEquals(302, autologin.statusCode(), autologin.body());
		assertEquals("/test/user/area", URI.create(location(autologin)).getPath());
		assertEquals("Logged in as " + TestDataService.USER_EMAIL, loggedAs(client));
	}

	@Test
	void resetEmptiesTheMailbox() throws Exception {
		HttpClient client = newClient();
		Map<String, String> fields = formFields(client, "/passwordReset", "passwordResetForm");
		fields.put("email", TestDataService.USER_EMAIL);
		post(client, "/yadaPasswordResetPost", fields);
		assertEquals(1, mailbox.messages().size());
		session.fixtures().reset();
		assertTrue(mailbox.messages().isEmpty());
	}
}
