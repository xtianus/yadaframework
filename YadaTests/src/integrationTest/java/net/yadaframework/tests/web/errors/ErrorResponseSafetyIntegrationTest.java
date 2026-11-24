package net.yadaframework.tests.web.errors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;
import net.yadaframework.tests.support.LogCapture;

/**
 * Exception text never reaches the client, before or after the response is committed. A response that fails after
 * commit may arrive truncated or aborted, but never completed with exception text.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErrorResponseSafetyIntegrationTest {
	private ApplicationSession session;

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

	/**
	 * @return the response body, or what was received before the connection was aborted
	 */
	private String fetch(String path, boolean ajax) throws Exception {
		HttpRequest.Builder builder = HttpRequest.newBuilder(session.application().resolve(path)).GET();
		if (ajax) {
			builder.header("X-Requested-With", "XMLHttpRequest");
		}
		try (HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
			return client.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
		} catch (IOException aborted) {
			// Truncated or aborted response: acceptable after commit
			return "";
		}
	}

	@Test
	void committedResponseNeverCarriesExceptionText() throws Exception {
		try (LogCapture logs = LogCapture.start()) {
			String body = fetch("/test/errors/committed", false);
			assertFalse(body.contains("secret-sql-detail"), body);
			assertFalse(body.contains("<script>alert"), body);
			assertTrue(logs.text().contains("secret-sql-detail"), "The detail must be in the server log");
		}
	}

	@Test
	void uncommittedAjaxErrorIsGeneric() throws Exception {
		String body = fetch("/test/errors/uncommitted", true);
		assertFalse(body.contains("secret-sql-detail"), body);
		assertFalse(body.contains("<script>alert"), body);
		assertTrue(body.contains("yadaError:"), body);
	}

	@Test
	void uncommittedPageErrorIsGeneric() throws Exception {
		String body = fetch("/test/errors/uncommitted", false);
		assertFalse(body.contains("secret-sql-detail"), body);
		assertFalse(body.contains("<script>alert"), body);
	}
}
