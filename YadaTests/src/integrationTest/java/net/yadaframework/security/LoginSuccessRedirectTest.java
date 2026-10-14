package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;

/**
 * "/yadaLoginSuccess?targetUrl=" (YadaLoginController) answers the ajax redirect document of YadaViews.AJAX_REDIRECT: the
 * target must be a local path, otherwise it becomes "/", and it is written as a JSON string that can't close the string,
 * add JSON properties or HTML markup, whatever the client sends.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoginSuccessRedirectTest {
	private ApplicationSession session;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
	}

	@AfterAll
	void stopApplication() {
		if (session != null) {
			session.close();
		}
	}

	private HttpResponse<String> loginSuccess(String targetUrl) throws Exception {
		AuthTestClient client = new AuthTestClient(session.application());
		return client.get("/yadaLoginSuccess?targetUrl=" + URLEncoder.encode(targetUrl, StandardCharsets.UTF_8));
	}

	/**
	 * @return the redirect target that yada.ajax.js reads from the answer
	 */
	private String redirectOf(HttpResponse<String> response) throws Exception {
		assertEquals(200, response.statusCode());
		String body = response.body().trim();
		assertTrue(body.startsWith("{\"redirect\":"), "yada.ajax.js would not see a redirect:\n" + body);
		JsonNode json = objectMapper.readTree(body);
		return json.get("redirect").asText();
	}

	@Test
	void localTargetIsKept() throws Exception {
		assertEquals("/test/user/area?tab=1&page=2#top", redirectOf(loginSuccess("/test/user/area?tab=1&page=2#top")));
	}

	@Test
	void markupInTheTargetIsEscaped() throws Exception {
		String payload = "/x?<script>alert(1)</script>";
		HttpResponse<String> response = loginSuccess(payload);
		assertFalse(response.body().contains("<script>"), response.body());
		assertFalse(response.body().contains("</script>"), response.body());
		assertEquals(payload, redirectOf(response), "The escaped value is still the same url for yada.ajax.js");
	}

	@Test
	void targetCanNotBreakTheJsonString() throws Exception {
		String payload = "/x?a=\",\"newTab\":\"true";
		HttpResponse<String> response = loginSuccess(payload);
		JsonNode json = objectMapper.readTree(response.body().trim());
		assertEquals(payload, json.get("redirect").asText());
		assertFalse("true".equals(json.get("newTab").asText()), response.body());
	}

	@Test
	void externalTargetsBecomeTheHomePage() throws Exception {
		for (String external : new String[] {"https://evil.example/", "//evil.example/x", "/\\evil.example", "javascript:alert(1)", "/%2F%2Fevil.example"}) {
			assertEquals("/", redirectOf(loginSuccess(external)), external);
		}
	}
}
