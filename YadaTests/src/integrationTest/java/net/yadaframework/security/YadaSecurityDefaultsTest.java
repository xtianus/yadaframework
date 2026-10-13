package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;

/**
 * Response headers and login failure responses of YadaSecurityConfig, checked through the real Spring Security
 * filter chain of the YadaTests application.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class YadaSecurityDefaultsTest {
	private static final String UNKNOWN_EMAIL = "nobody-here@yts.test";
	private static final String SUBMITTED_PASSWORD = "Submitted-Secret-4711";
	private static final String CONTENT_FIXTURE = "securityheaders/content-marker.txt";
	private static final int MAX_FAILED_ATTEMPTS = 2;

	private ApplicationSession session;
	private YadaConfiguration config;

	@BeforeAll
	void startApplication() throws Exception {
		session = ApplicationSession.start(run -> run.setConfigurationValue("security/maxFailedAttempts", String.valueOf(MAX_FAILED_ATTEMPTS)), application -> application.getBean(TestDataService.class)::seedDefaults);
		config = session.application().getBean(YadaConfiguration.class);
		Path contentFile = config.getContentsFolder().toPath().resolve(CONTENT_FIXTURE);
		Files.createDirectories(contentFile.getParent());
		Files.writeString(contentFile, "content");
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

	private HttpResponse<String> get(HttpClient client, String pathAndQuery, String... headers) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(session.application().resolve(pathAndQuery)).GET();
		if (headers.length > 0) {
			builder.headers(headers);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private HttpResponse<String> post(HttpClient client, String path, Map<String, String> fields, String... headers) throws Exception {
		String form = fields.entrySet().stream().map(field -> URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
		HttpRequest.Builder builder = HttpRequest.newBuilder(session.application().resolve(path)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form));
		if (headers.length > 0) {
			builder.headers(headers);
		}
		return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	/**
	 * Submits the login form of a fresh session
	 */
	private HttpResponse<String> login(HttpClient client, String email, String password, String... headers) throws Exception {
		HttpResponse<String> page = get(client, "/login");
		assertEquals(200, page.statusCode(), page.body());
		Element form = Jsoup.parse(page.body()).selectFirst("form[data-testid=loginForm]");
		assertNotNull(form);
		Map<String, String> fields = new LinkedHashMap<>();
		for (Element input : form.select("input[name]")) {
			fields.put(input.attr("name"), input.attr("value"));
		}
		fields.put("username", email);
		fields.put("password", password);
		return post(client, "/loginPost", fields, headers);
	}

	/**
	 * Removes the per-session CSRF values and the echoed username, so that two failure pages can be compared
	 */
	private static String comparableBody(String body, String username) {
		Document document = Jsoup.parse(body);
		document.select("input[name=_csrf], meta[name=_csrf]").remove();
		return document.outerHtml().replace(username, "USERNAME");
	}

	private static void assertSecurityHeaders(HttpResponse<String> response) {
		String description = response.request().uri() + " (" + response.statusCode() + ")";
		assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(null), description);
		assertEquals("DENY", response.headers().firstValue("X-Frame-Options").orElse(null), description);
		assertEquals("same-origin", response.headers().firstValue("Referrer-Policy").orElse(null), description);
		// HSTS is only written on secure requests, and the test server has no TLS connector
		assertTrue(response.headers().firstValue("Strict-Transport-Security").isEmpty(), description);
	}

	private static void assertNotCached(HttpResponse<String> response) {
		String cacheControl = response.headers().firstValue("Cache-Control").orElse("");
		assertTrue(cacheControl.contains("no-cache") && cacheControl.contains("no-store"), response.request().uri() + " Cache-Control: " + cacheControl);
	}

	@Test
	void htmlPagesCarryTheDefaultSecurityHeaders() throws Exception {
		HttpResponse<String> response = get(newClient(), "/login");
		assertEquals(200, response.statusCode());
		assertSecurityHeaders(response);
		assertNotCached(response);
	}

	@Test
	void ajaxResponsesCarryTheDefaultSecurityHeaders() throws Exception {
		HttpResponse<String> response = get(newClient(), "/login", "X-Requested-With", "XMLHttpRequest");
		assertEquals(200, response.statusCode());
		assertSecurityHeaders(response);
		assertNotCached(response);
	}

	@Test
	void redirectsCarryTheDefaultSecurityHeaders() throws Exception {
		HttpResponse<String> response = get(newClient(), "/test/user/area");
		assertEquals(302, response.statusCode());
		assertSecurityHeaders(response);
		assertNotCached(response);
	}

	@Test
	void errorResponsesCarryTheDefaultSecurityHeaders() throws Exception {
		HttpResponse<String> response = get(newClient(), "/test/securityheaders/doesNotExist");
		assertEquals(404, response.statusCode());
		assertSecurityHeaders(response);
		assertNotCached(response);
	}

	@Test
	void staticResourcesKeepTheirConfiguredCaching() throws Exception {
		HttpClient client = newClient();
		String hundredDays = "max-age=8640000";
		Map<String, String> expected = new LinkedHashMap<>();
		expected.put("/res-" + config.getApplicationBuild() + "/securityheaders/marker.txt", hundredDays);
		expected.put("/static/jquery-3.7.1.min.js", hundredDays);
		expected.put("/yadares-" + config.getYadaVersion() + "/js/yada.js", hundredDays);
		expected.put(config.getContentUrl() + "/" + CONTENT_FIXTURE, hundredDays);
		expected.put("/robots.txt", "max-age=86400");
		for (Map.Entry<String, String> resource : expected.entrySet()) {
			HttpResponse<String> response = get(client, resource.getKey());
			assertEquals(200, response.statusCode(), resource.getKey());
			assertEquals(resource.getValue(), response.headers().firstValue("Cache-Control").orElse(null), resource.getKey());
			assertTrue(response.headers().firstValue("Pragma").isEmpty(), resource.getKey());
			assertTrue(response.headers().firstValue("Expires").isEmpty(), resource.getKey());
			assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(null), resource.getKey());
		}
	}

	@Test
	void unknownUserAndWrongPasswordGiveTheSamePage() throws Exception {
		HttpResponse<String> unknownUser = login(newClient(), UNKNOWN_EMAIL, SUBMITTED_PASSWORD);
		HttpResponse<String> wrongPassword = login(newClient(), TestDataService.USER_EMAIL, SUBMITTED_PASSWORD);

		assertEquals(200, wrongPassword.statusCode());
		assertEquals(wrongPassword.statusCode(), unknownUser.statusCode());
		assertNull(unknownUser.headers().firstValue("Location").orElse(null));
		assertNotNull(Jsoup.parse(unknownUser.body()).selectFirst("[data-testid=loginError]"));
		assertFalse(unknownUser.body().contains(SUBMITTED_PASSWORD), "The submitted password is echoed");
		assertFalse(wrongPassword.body().contains(SUBMITTED_PASSWORD), "The submitted password is echoed");
		assertEquals(comparableBody(wrongPassword.body(), TestDataService.USER_EMAIL), comparableBody(unknownUser.body(), UNKNOWN_EMAIL));
	}

	@Test
	void unknownUserAndWrongPasswordGiveTheSameAjaxResponse() throws Exception {
		HttpResponse<String> unknownUser = login(newClient(), UNKNOWN_EMAIL, SUBMITTED_PASSWORD, "X-Requested-With", "XMLHttpRequest");
		HttpResponse<String> wrongPassword = login(newClient(), TestDataService.USER_EMAIL, SUBMITTED_PASSWORD, "X-Requested-With", "XMLHttpRequest");

		assertEquals(200, wrongPassword.statusCode(), wrongPassword.body());
		assertEquals(wrongPassword.statusCode(), unknownUser.statusCode());
		Document unknownUserPage = Jsoup.parse(unknownUser.body());
		assertEquals("loginErrorFlag", unknownUserPage.selectFirst("[data-testid=ajaxLoginError]").text());
		assertTrue(unknownUserPage.select("[data-testid=usernameNotFound]").isEmpty(), "The unknown-user flag is set");
		assertTrue(unknownUserPage.select("[data-testid=passwordAttribute]").isEmpty(), "The submitted password is a request attribute");
		assertFalse(unknownUser.body().contains(SUBMITTED_PASSWORD), "The submitted password is echoed");
		assertEquals(comparableBody(wrongPassword.body(), TestDataService.USER_EMAIL), comparableBody(unknownUser.body(), UNKNOWN_EMAIL));
	}

	@Test
	void lockoutStillStopsTheRightPasswordAndReportsTheMinutesLeft() throws Exception {
		for (int attempt = 0; attempt <= MAX_FAILED_ATTEMPTS; attempt++) {
			login(newClient(), TestDataService.USER_EMAIL, SUBMITTED_PASSWORD);
		}
		HttpResponse<String> locked = login(newClient(), TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD, "X-Requested-With", "XMLHttpRequest");

		assertEquals(200, locked.statusCode(), locked.body());
		Document page = Jsoup.parse(locked.body());
		assertNotNull(page.selectFirst("[data-testid=ajaxLoginError]"), locked.body());
		Element lockout = page.selectFirst("[data-testid=lockoutMinutes]");
		assertNotNull(lockout, "The lockout is not reported: " + locked.body());
		assertTrue(Long.parseLong(lockout.text()) > 0, lockout.text());
	}
}
