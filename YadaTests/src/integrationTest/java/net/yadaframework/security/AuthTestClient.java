package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

import net.yadaframework.tests.support.TestApplication;

/**
 * A browser-like HTTP client of the test application for the authentication tests: it keeps its own cookies (so one
 * instance is one browser session), never follows redirects and reads forms with their CSRF token.
 */
final class AuthTestClient {
	private final TestApplication application;
	private final HttpClient client;

	AuthTestClient(TestApplication application) {
		this.application = application;
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		this.client = HttpClient.newBuilder().cookieHandler(cookieManager).followRedirects(HttpClient.Redirect.NEVER).build();
	}

	HttpResponse<String> get(String pathAndQuery) throws Exception {
		return client.send(HttpRequest.newBuilder(application.resolve(pathAndQuery)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	HttpResponse<String> post(String pathAndQuery, Map<String, String> fields) throws Exception {
		String form = fields.entrySet().stream().map(field -> URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
		HttpRequest request = HttpRequest.newBuilder(application.resolve(pathAndQuery)).header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form)).build();
		return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	/**
	 * Loads a page and returns the fields of one of its forms, including the CSRF token
	 * @return the field values, or null when the page has no such form
	 */
	Map<String, String> formFieldsOrNull(String pagePath, String formTestId) throws Exception {
		HttpResponse<String> page = get(pagePath);
		Element form = Jsoup.parse(page.body()).selectFirst("form[data-testid=" + formTestId + "]");
		if (form == null) {
			return null;
		}
		return fieldsOf(form);
	}

	Map<String, String> formFields(String pagePath, String formTestId) throws Exception {
		HttpResponse<String> page = get(pagePath);
		assertEquals(200, page.statusCode(), page.body());
		Element form = Jsoup.parse(page.body()).selectFirst("form[data-testid=" + formTestId + "]");
		assertNotNull(form, "No form " + formTestId + " in " + pagePath);
		return fieldsOf(form);
	}

	static Map<String, String> fieldsOf(Element form) {
		Map<String, String> fields = new LinkedHashMap<>();
		for (Element input : form.select("input[name]")) {
			fields.put(input.attr("name"), input.attr("value"));
		}
		return fields;
	}

	/**
	 * @return the "Logged in as ..." text of the home page, or null when not logged in
	 */
	String loggedAs() throws Exception {
		Document home = Jsoup.parse(get("/test/home").body());
		Element loggedAs = home.selectFirst("[data-testid=loggedAs]");
		return loggedAs == null ? null : loggedAs.text();
	}

	/**
	 * @return the visible text of the home page, where the flash notifications are shown
	 */
	String homeText() throws Exception {
		return Jsoup.parse(get("/test/home").body()).text();
	}

	/**
	 * @return the CSRF token of this client's session, as the pages of the session contain it
	 */
	String csrfToken() throws Exception {
		Element meta = Jsoup.parse(get("/test/home").body()).selectFirst("meta[name=_csrf]");
		assertNotNull(meta, "No CSRF meta tag on the home page");
		return meta.attr("content");
	}

	HttpResponse<String> login(String email, String password) throws Exception {
		Map<String, String> fields = formFields("/login", "loginForm");
		fields.put("username", email);
		fields.put("password", password);
		return post("/loginPost", fields);
	}

	static String location(HttpResponse<String> response) {
		return response.headers().firstValue("Location").orElse(null);
	}

	/**
	 * @return the path and query of an absolute link found in an email
	 */
	static String pathAndQuery(String link) {
		URI uri = URI.create(link);
		return uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
	}

	/**
	 * @param link a link whose path ends with "&lt;id>-&lt;secret>"
	 * @return the token part "&lt;id>-&lt;secret>" of the path
	 */
	static String tokenOf(String link) {
		String path = URI.create(link).getRawPath();
		return path.substring(path.lastIndexOf('/') + 1);
	}

	/**
	 * @return the secret part of a token link
	 */
	static String secretOf(String link) {
		String token = tokenOf(link);
		return token.substring(token.indexOf('-') + 1);
	}

	/**
	 * @return the id part of a token link
	 */
	static long idOf(String link) {
		String token = tokenOf(link);
		return Long.parseLong(token.substring(0, token.indexOf('-')));
	}
}
