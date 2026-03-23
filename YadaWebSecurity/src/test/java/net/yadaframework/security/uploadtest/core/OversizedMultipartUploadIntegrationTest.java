package net.yadaframework.security.uploadtest.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.core.YadaEmbeddedTomcatTestServer;
import net.yadaframework.core.YadaTestConfigurationScope;

/**
 * Exercises oversized multipart uploads against a real embedded servlet container.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OversizedMultipartUploadIntegrationTest {
	private TestServerContext serverContext;

	@BeforeAll
	void setUp() throws Exception {
		serverContext = startServer();
	}

	@AfterAll
	void tearDown() throws Exception {
		if (serverContext != null) {
			serverContext.close();
		}
	}

	@Test
	void formPageLoadsAndContainsCsrfField() throws Exception {
		HttpResponse<String> response = serverContext.client().send(
			HttpRequest.newBuilder(serverContext.server().resolve("/test/upload/form")).GET().build(),
			HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		assertEquals(200, response.statusCode());
		Document document = Jsoup.parse(response.body(), response.uri().toString());
		Element csrfInput = document.selectFirst("input[type=hidden][name][value]");
		assertNotNull(csrfInput);
		assertFalse(csrfInput.attr("value").isBlank());
	}

	/**
	 * Verifies that the multipart form action keeps the query before the fragment.
	 * @throws Exception if the form cannot be loaded
	 */
	@Test
	void formActionKeepsCsrfQueryBeforeFragment() throws Exception {
		HttpResponse<String> response = serverContext.client().send(
			HttpRequest.newBuilder(serverContext.server().resolve("/test/upload/form")).GET().build(),
			HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		assertEquals(200, response.statusCode());
		Document document = Jsoup.parse(response.body(), response.uri().toString());
		Element form = document.selectFirst("form[action]");
		assertNotNull(form);
		assertEquals("/test/upload/submit?_csrf=" + document.selectFirst("input[type=hidden][name][value]").attr("value") + "#done", form.attr("action"));
	}

	@Test
	void oversizedMultipartDoesNotReturn403AndReachesController() throws Exception {
		HttpResponse<String> formResponse = serverContext.client().send(
			HttpRequest.newBuilder(serverContext.server().resolve("/test/upload/form")).GET().build(),
			HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		assertEquals(200, formResponse.statusCode());
		Document document = Jsoup.parse(formResponse.body(), formResponse.uri().toString());
		Element form = document.selectFirst("form[action]");
		Element csrfInput = document.selectFirst("input[type=hidden][name][value]");
		assertNotNull(form);
		assertNotNull(csrfInput);

		URI submitUri = URI.create(form.absUrl("action"));
		String csrfName = csrfInput.attr("name");
		String csrfToken = csrfInput.attr("value");
		String boundary = "----YadaBoundary" + UUID.randomUUID().toString().replace("-", "");
		byte[] requestBody = buildMultipartBody(boundary, csrfName, csrfToken, new byte[] { 0x01, 0x02 });

		HttpResponse<String> response = serverContext.client().send(
			HttpRequest.newBuilder(submitUri)
				.header("Content-Type", "multipart/form-data; boundary=" + boundary)
				.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
				.build(),
			HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

		assertEquals(413, response.statusCode());
		assertNotEquals(403, response.statusCode());
		assertEquals("LIMIT_EXCEEDED", response.body());
	}

	private TestServerContext startServer() throws Exception {
		YadaTestConfigurationScope configurationScope = new YadaTestConfigurationScope(getResourcePath("configuration.xml"));
		try {
			YadaEmbeddedTomcatTestServer server = new YadaEmbeddedTomcatTestServer(getResourcePath("itest-webapp"));
			try {
				server.start();
				return new TestServerContext(configurationScope, server, newHttpClient());
			} catch (Exception e) {
				server.close();
				throw e;
			}
		} catch (Exception e) {
			configurationScope.close();
			throw e;
		}
	}

	private Path getResourcePath(String resourcePath) throws Exception {
		URL resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
		assertNotNull(resource, "Missing test resource " + resourcePath);
		return Path.of(resource.toURI());
	}

	private HttpClient newHttpClient() {
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		return HttpClient.newBuilder().cookieHandler(cookieManager).followRedirects(HttpClient.Redirect.NORMAL).build();
	}

	private byte[] buildMultipartBody(String boundary, String csrfName, String csrfToken, byte[] fileContent) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		writeTextPart(outputStream, boundary, csrfName, csrfToken);
		writeFilePart(outputStream, boundary, "upfile", "too-big.txt", fileContent);
		outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		return outputStream.toByteArray();
	}

	private void writeTextPart(ByteArrayOutputStream outputStream, String boundary, String fieldName, String fieldValue) throws IOException {
		outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
		outputStream.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
		outputStream.write(fieldValue.getBytes(StandardCharsets.UTF_8));
		outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
	}

	private void writeFilePart(ByteArrayOutputStream outputStream, String boundary, String fieldName, String filename, byte[] fileContent) throws IOException {
		outputStream.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
		outputStream.write(("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
		outputStream.write("Content-Type: text/plain\r\n\r\n".getBytes(StandardCharsets.UTF_8));
		outputStream.write(fileContent);
		outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Groups the server and its scoped configuration for one test run.
	 */
	private record TestServerContext(YadaTestConfigurationScope configurationScope, YadaEmbeddedTomcatTestServer server, HttpClient client) implements AutoCloseable {

		@Override
		public void close() throws Exception {
			server.close();
			configurationScope.close();
		}
	}
}
