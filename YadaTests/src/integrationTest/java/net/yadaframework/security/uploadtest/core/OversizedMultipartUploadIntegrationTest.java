package net.yadaframework.security.uploadtest.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaEmbeddedTomcatTestServer;
import net.yadaframework.core.YadaTestConfigurationScope;

/**
 * Exercises oversized multipart uploads against a real embedded servlet container.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OversizedMultipartUploadIntegrationTest {
	private static final String SUBMIT_PATH = "/test/upload/submit";
	/** Within the 1024 bytes of maxFileUploadSizeBytes */
	private static final byte[] SMALL_FILE = new byte[] { 0x01, 0x02 };
	/** Over the 1024 bytes of maxFileUploadSizeBytes */
	private static final byte[] LARGE_FILE = new byte[4096];

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
	 * No rendered form action carries the CSRF token: queries and fragments stay as written, the hidden field
	 * carries the token, and yada:actionUpload never adds it for another origin.
	 * @throws Exception if the form cannot be loaded
	 */
	@Test
	void formActionsContainNoCsrfTokenAndKeepQueryAndFragment() throws Exception {
		Document document = loadForm();
		String token = document.selectFirst("meta[name=_csrf]").attr("content");
		assertFalse(token.isBlank());

		assertEquals("/test/upload/submit#done", form(document, "plainForm").attr("action"));
		assertEquals("/test/upload/submit?x=1#done", form(document, "legacyForm").attr("action"));
		assertEquals(1, form(document, "legacyForm").select("input[type=hidden][name=_csrf]").size());
		assertEquals("/test/upload/submit?x=2", form(document, "actionUploadForm").attr("action"));
		assertEquals(1, form(document, "actionUploadForm").select("input[type=hidden][name=_csrf]").size());
		assertEquals("https://elsewhere.example/upload?x=3", form(document, "externalForm").attr("action"));
		assertTrue(form(document, "externalForm").select("input[name=_csrf]").isEmpty(), "The token is sent to another origin");
		for (Element form : document.select("form")) {
			assertFalse(form.attr("action").contains("_csrf"), form.attr("action"));
			assertFalse(form.attr("action").contains(token), form.attr("action"));
		}
	}

	@Test
	void smallUploadWithTheHiddenFieldIsAccepted() throws Exception {
		Csrf csrf = csrf();
		int accepted = acceptedUploads();
		HttpResponse<String> response = upload(csrf.name(), csrf.token(), null, null, SMALL_FILE);
		assertEquals(200, response.statusCode(), response.body());
		assertEquals("UPLOAD_OK " + SMALL_FILE.length, response.body());
		assertEquals(accepted + 1, acceptedUploads());
	}

	@Test
	void smallUploadWithTheHeaderIsAccepted() throws Exception {
		Csrf csrf = csrf();
		int accepted = acceptedUploads();
		HttpResponse<String> response = upload(null, null, csrf.header(), csrf.token(), SMALL_FILE);
		assertEquals(200, response.statusCode(), response.body());
		assertEquals(accepted + 1, acceptedUploads());
	}

	@Test
	void missingCsrfIsRejectedBeforeTheController() throws Exception {
		csrf();
		int accepted = acceptedUploads();
		HttpResponse<String> response = upload(null, null, null, null, SMALL_FILE);
		assertEquals(403, response.statusCode());
		assertEquals(accepted, acceptedUploads());
	}

	@Test
	void invalidCsrfIsRejectedBeforeTheController() throws Exception {
		Csrf csrf = csrf();
		int accepted = acceptedUploads();
		assertEquals(403, upload(csrf.name(), "not-the-token", null, null, SMALL_FILE).statusCode());
		assertEquals(403, upload(null, null, csrf.header(), "not-the-token", SMALL_FILE).statusCode());
		assertEquals(accepted, acceptedUploads());
	}

	@Test
	void oversizedUploadWithTheHeaderReachesTheLimitHandler() throws Exception {
		Csrf csrf = csrf();
		int accepted = acceptedUploads();
		HttpResponse<String> response = upload(csrf.name(), csrf.token(), csrf.header(), csrf.token(), LARGE_FILE);
		assertEquals(413, response.statusCode());
		assertEquals("LIMIT_EXCEEDED", response.body());
		assertEquals(accepted, acceptedUploads());
	}

	/**
	 * A plain form over the limit can't be CSRF-validated because its hidden field is in the unparsed body:
	 * it gets a controlled 413 without reaching the controller.
	 */
	@Test
	void oversizedPlainFormGetsAControlledErrorWithoutReachingTheController() throws Exception {
		Csrf csrf = csrf();
		int accepted = acceptedUploads();
		HttpResponse<String> response = upload(csrf.name(), csrf.token(), null, null, LARGE_FILE);
		assertEquals(413, response.statusCode());
		assertNotEquals("LIMIT_EXCEEDED", response.body());
		assertEquals(accepted, acceptedUploads());
	}

	@Test
	void oversizedUploadWithAnInvalidHeaderIsForbidden() throws Exception {
		Csrf csrf = csrf();
		int accepted = acceptedUploads();
		HttpResponse<String> response = upload(null, null, csrf.header(), "not-the-token", LARGE_FILE);
		assertEquals(403, response.statusCode());
		assertEquals(accepted, acceptedUploads());
	}

	/**
	 * A body whose parts don't match the declared boundary parses as an empty multipart request: without a CSRF
	 * header it is rejected, with a valid header the controller receives no file.
	 */
	@Test
	void multipartWithTheWrongBoundaryCarriesNoFieldsAndNoFile() throws Exception {
		Csrf csrf = csrf();
		byte[] malformed = ("--wrongBoundary\r\nContent-Disposition: form-data; name=\"" + csrf.name() + "\"\r\n\r\n" + csrf.token() + "\r\n--wrongBoundary--\r\n").getBytes(StandardCharsets.UTF_8);
		int accepted = acceptedUploads();
		HttpResponse<String> withoutHeader = send(HttpRequest.newBuilder(serverContext.server().resolve(SUBMIT_PATH))
			.header("Content-Type", "multipart/form-data; boundary=expectedBoundary")
			.POST(HttpRequest.BodyPublishers.ofByteArray(malformed)));
		assertEquals(403, withoutHeader.statusCode());
		assertEquals(accepted, acceptedUploads());
		HttpResponse<String> withHeader = send(HttpRequest.newBuilder(serverContext.server().resolve(SUBMIT_PATH))
			.header("Content-Type", "multipart/form-data; boundary=expectedBoundary")
			.header(csrf.header(), csrf.token())
			.POST(HttpRequest.BodyPublishers.ofByteArray(malformed)));
		assertEquals(200, withHeader.statusCode(), withHeader.body());
		assertEquals("UPLOAD_OK 0", withHeader.body());
	}

	/**
	 * A multipart content type without a boundary can't be parsed at all
	 */
	@Test
	void multipartWithoutBoundaryNeverReachesTheController() throws Exception {
		Csrf csrf = csrf();
		int accepted = acceptedUploads();
		HttpResponse<String> withHeader = send(HttpRequest.newBuilder(serverContext.server().resolve(SUBMIT_PATH))
			.header("Content-Type", "multipart/form-data")
			.header(csrf.header(), csrf.token())
			.POST(HttpRequest.BodyPublishers.ofString("garbage")));
		HttpResponse<String> withoutHeader = send(HttpRequest.newBuilder(serverContext.server().resolve(SUBMIT_PATH))
			.header("Content-Type", "multipart/form-data")
			.POST(HttpRequest.BodyPublishers.ofString("garbage")));
		assertTrue(withHeader.statusCode() >= 400, "Status " + withHeader.statusCode() + ": " + withHeader.body());
		assertTrue(withoutHeader.statusCode() >= 400, "Status " + withoutHeader.statusCode() + ": " + withoutHeader.body());
		assertEquals(accepted, acceptedUploads());
	}

	@Test
	void multipartTempFilesAreDeleted() throws Exception {
		Csrf csrf = csrf();
		upload(csrf.name(), csrf.token(), null, null, SMALL_FILE);
		upload(null, null, null, null, SMALL_FILE);
		upload(csrf.name(), csrf.token(), null, null, LARGE_FILE);
		upload(null, null, csrf.header(), csrf.token(), LARGE_FILE);
		File uploadsFolder = YadaAppConfig.getStaticConfig().getUploadsFolder();
		long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
		String[] leftovers = uploadsFolder.list();
		while (leftovers != null && leftovers.length > 0 && System.nanoTime() < deadline) {
			Thread.sleep(100);
			leftovers = uploadsFolder.list();
		}
		assertTrue(leftovers == null || leftovers.length == 0, "Temporary files left in " + uploadsFolder + ": " + Arrays.toString(leftovers));
	}

	@Test
	void oversizedMultipartDoesNotReturn403AndReachesController() throws Exception {
		Csrf csrf = csrf();
		HttpResponse<String> response = upload(null, null, csrf.header(), csrf.token(), LARGE_FILE);

		assertEquals(413, response.statusCode());
		assertNotEquals(403, response.statusCode());
		assertEquals("LIMIT_EXCEEDED", response.body());
	}

	private Document loadForm() throws Exception {
		HttpResponse<String> response = send(HttpRequest.newBuilder(serverContext.server().resolve("/test/upload/form")).GET());
		assertEquals(200, response.statusCode());
		return Jsoup.parse(response.body(), response.uri().toString());
	}

	private static Element form(Document document, String testId) {
		Element result = document.selectFirst("form[data-testid=" + testId + "]");
		assertNotNull(result, testId);
		return result;
	}

	/**
	 * Loads the form page and reads the CSRF names and token from its meta tags
	 */
	private Csrf csrf() throws Exception {
		Document document = loadForm();
		return new Csrf(document.selectFirst("meta[name=_csrf_parameter]").attr("content"), document.selectFirst("meta[name=_csrf_header]").attr("content"), document.selectFirst("meta[name=_csrf]").attr("content"));
	}

	private int acceptedUploads() throws Exception {
		return Integer.parseInt(send(HttpRequest.newBuilder(serverContext.server().resolve("/test/upload/accepted")).GET()).body());
	}

	private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
		return serverContext.client().send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	/**
	 * Posts a multipart upload to the submit endpoint
	 * @param fieldName name of the CSRF form field, null for none
	 * @param fieldValue value of the CSRF form field
	 * @param headerName name of the CSRF header, null for none
	 * @param headerValue value of the CSRF header
	 * @param fileContent content of the uploaded file
	 */
	private HttpResponse<String> upload(String fieldName, String fieldValue, String headerName, String headerValue, byte[] fileContent) throws Exception {
		String boundary = "----YadaBoundary" + UUID.randomUUID().toString().replace("-", "");
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		if (fieldName != null) {
			writeTextPart(outputStream, boundary, fieldName, fieldValue);
		}
		writeFilePart(outputStream, boundary, "upfile", "upload.txt", fileContent);
		outputStream.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
		HttpRequest.Builder builder = HttpRequest.newBuilder(serverContext.server().resolve(SUBMIT_PATH))
			.header("Content-Type", "multipart/form-data; boundary=" + boundary)
			.POST(HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray()));
		if (headerName != null) {
			builder.header(headerName, headerValue);
		}
		return send(builder);
	}

	private TestServerContext startServer() throws Exception {
		YadaTestConfigurationScope configurationScope = new YadaTestConfigurationScope(getResourcePath("uploadtest-configuration.xml"));
		try {
			YadaEmbeddedTomcatTestServer server = new YadaEmbeddedTomcatTestServer(getResourcePath("uploadtest-webapp"));
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
	 * CSRF parameter name, header name and token of the current session
	 */
	private record Csrf(String name, String header, String token) {
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
