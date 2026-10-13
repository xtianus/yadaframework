package net.yadaframework.tests.web.datatable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import net.yadaframework.security.persistence.entity.YadaUserCredentials;
import net.yadaframework.security.persistence.repository.YadaUserCredentialsDao;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;
import net.yadaframework.tests.support.LogCapture;

/**
 * DataTables requests reach Hibernate only through the server-side table policy: tampered column paths, order
 * directions, order indexes, search flags and extra attributes are rejected (or ignored, for search flags) before any
 * query runs, and no credential field can be read. Both the framework endpoint /yadaDataTableData and a custom endpoint
 * are exercised on the embedded MariaDB through the real Spring Security filter chain.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DataTableSecurityIntegrationTest {
	private static final String PAGE = "/test/admin/datatable/users";
	private static final String HIBERNATE_SQL_LOGGER = "org.hibernate.SQL";
	private static final ObjectMapper JSON = new ObjectMapper();

	/**
	 * The two data endpoints, with the columns that the page sends to each of them
	 */
	enum Endpoint {
		CONFIGURED("/yadaDataTableData", DataTableScenarioController.CONFIGURED_TABLE_ID),
		CUSTOM("/test/admin/datatable/userTablePage", DataTableScenarioController.CUSTOM_TABLE_ID);

		final String path;
		final String tableId;

		Endpoint(String path, String tableId) {
			this.path = path;
			this.tableId = tableId;
		}
	}

	/** Column positions of both tables: 0 is the selection checkbox */
	private static final int ID_COLUMN = 1;
	private static final int NICKNAME_COLUMN = 2;
	private static final int USERNAME_COLUMN = 3;
	private static final int LAST_LOGIN_COLUMN = 4;

	private ApplicationSession session;
	private TestDataService testDataService;
	private String adminPasswordHash;

	@BeforeAll
	void startApplication() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
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
		YadaUserCredentials adminCredentials = session.application().getBean(YadaUserCredentialsDao.class).findFirstByUsername(TestDataService.ADMIN_EMAIL);
		adminPasswordHash = adminCredentials.getPassword();
		assertTrue(adminPasswordHash != null && adminPasswordHash.length() > 20, "The seeded password is not hashed");
	}

	/**
	 * A logged-in browser that has opened the table page
	 */
	private final class Browser {
		final CookieManager cookieManager = new CookieManager();
		String csrfToken;
		String csrfHeader;

		Browser(String email, String password) throws Exception {
			cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
			HttpResponse<String> loginPage = get("/login");
			Element loginForm = Jsoup.parse(loginPage.body()).selectFirst("form[data-testid=loginForm]");
			Map<String, String> fields = new LinkedHashMap<>();
			for (Element input : loginForm.select("input[name]")) {
				fields.put(input.attr("name"), input.attr("value"));
			}
			fields.put("username", email);
			fields.put("password", password);
			HttpResponse<String> login = post("/loginPost", fields, null, null);
			assertEquals(302, login.statusCode(), login.body());
			// The CSRF token changes at login: read the new one from a page
			readCsrf(TestDataService.ADMIN_EMAIL.equals(email) ? PAGE : "/test/home");
		}

		void readCsrf(String pagePath) throws Exception {
			HttpResponse<String> page = get(pagePath);
			assertEquals(200, page.statusCode(), page.body());
			csrfToken = Jsoup.parse(page.body()).selectFirst("meta[name=_csrf]").attr("content");
			csrfHeader = Jsoup.parse(page.body()).selectFirst("meta[name=_csrf_header]").attr("content");
		}

		/**
		 * Sends each request on a new connection, sharing only the cookies: a reused keep-alive connection
		 * that the server has just closed would fail the request with "header parser received no bytes".
		 */
		private HttpResponse<String> send(HttpRequest request) throws Exception {
			try (HttpClient client = HttpClient.newBuilder().cookieHandler(cookieManager).followRedirects(HttpClient.Redirect.NEVER).build()) {
				return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			}
		}

		HttpResponse<String> get(String pathAndQuery) throws Exception {
			return send(HttpRequest.newBuilder(session.application().resolve(pathAndQuery)).GET().build());
		}

		HttpResponse<String> post(String path, Map<String, String> fields, String headerName, String headerValue) throws Exception {
			String form = fields.entrySet().stream().map(field -> URLEncoder.encode(field.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(field.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
			HttpRequest.Builder builder = HttpRequest.newBuilder(session.application().resolve(path)).header("Content-Type", "application/x-www-form-urlencoded").header("X-Requested-With", "XMLHttpRequest");
			if (headerName != null) {
				builder.header(headerName, headerValue);
			}
			return send(builder.POST(HttpRequest.BodyPublishers.ofString(form)).build());
		}

		HttpResponse<String> table(Endpoint endpoint, TableRequest tableRequest) throws Exception {
			return post(endpoint.path, tableRequest.fields(), csrfHeader, csrfToken);
		}
	}

	/**
	 * The parameters that DataTables sends, as jQuery.param encodes them
	 */
	private static final class TableRequest {
		final List<String[]> columns = new ArrayList<>(); // data, name, searchable, orderable
		final List<String[]> orders = new ArrayList<>(); // column, dir
		final List<String> extraJsonAttributes = new ArrayList<>();
		String searchValue = "";
		String dataTableId;

		/**
		 * A request with one data column for each path, the first one sorted ascending
		 */
		static TableRequest with(String tableId, String... paths) {
			TableRequest result = new TableRequest();
			result.dataTableId = tableId;
			for (String path : paths) {
				result.columns.add(new String[] { path, path, "true", "true" });
			}
			result.orders.add(new String[] { "0", "asc" });
			return result;
		}

		/**
		 * The request that the page sends for a table
		 */
		static TableRequest of(Endpoint endpoint) {
			TableRequest result = new TableRequest();
			result.dataTableId = endpoint.tableId;
			result.columns.add(new String[] { "", "", "false", "false" });
			result.columns.add(new String[] { "id", "id", "true", "true" });
			result.columns.add(new String[] { "nickname", "nickname", "false", "true" });
			result.columns.add(new String[] { "userCredentials.username", "userCredentials.username", "true", "true" });
			result.columns.add(new String[] { "userCredentials.lastSuccessfulLogin", "userCredentials.lastSuccessfulLogin", "true", "false" });
			if (endpoint == Endpoint.CONFIGURED) {
				result.columns.add(new String[] { "userCredentials.enabled", "userCredentials.enabled", "true", "false" });
			}
			result.orders.add(new String[] { String.valueOf(USERNAME_COLUMN), "asc" });
			return result;
		}

		TableRequest column(int index, String data, String name) {
			columns.get(index)[0] = data;
			columns.get(index)[1] = name;
			return this;
		}

		Map<String, String> fields() {
			Map<String, String> fields = new LinkedHashMap<>();
			fields.put("draw", "1");
			for (int i = 0; i < columns.size(); i++) {
				String[] column = columns.get(i);
				fields.put("columns[" + i + "][data]", column[0]);
				fields.put("columns[" + i + "][name]", column[1]);
				fields.put("columns[" + i + "][searchable]", column[2]);
				fields.put("columns[" + i + "][orderable]", column[3]);
				fields.put("columns[" + i + "][search][value]", "");
				fields.put("columns[" + i + "][search][regex]", "false");
			}
			for (int i = 0; i < orders.size(); i++) {
				fields.put("order[" + i + "][column]", orders.get(i)[0]);
				fields.put("order[" + i + "][dir]", orders.get(i)[1]);
			}
			fields.put("start", "0");
			fields.put("length", "10");
			fields.put("search[value]", searchValue);
			fields.put("search[regex]", "false");
			for (int i = 0; i < extraJsonAttributes.size(); i++) {
				fields.put("extraJsonAttributes[" + i + "]", extraJsonAttributes.get(i));
			}
			if (dataTableId != null) {
				fields.put("dataTableId", dataTableId);
			}
			return fields;
		}
	}

	private Browser admin() throws Exception {
		return new Browser(TestDataService.ADMIN_EMAIL, TestDataService.ADMIN_PASSWORD);
	}

	/**
	 * Sends a request while recording the SQL that Hibernate runs
	 * @return the response and the captured log
	 */
	private Object[] sendRecordingSql(Browser browser, Endpoint endpoint, TableRequest tableRequest) throws Exception {
		Logger sqlLogger = (Logger) LoggerFactory.getLogger(HIBERNATE_SQL_LOGGER);
		Level previousLevel = sqlLogger.getLevel();
		sqlLogger.setLevel(Level.DEBUG);
		try (LogCapture logs = LogCapture.start()) {
			HttpResponse<String> response = browser.table(endpoint, tableRequest);
			return new Object[] { response, logs.text() };
		} finally {
			sqlLogger.setLevel(previousLevel);
		}
	}

	private static String sqlStatements(String logText) {
		return logText.lines().filter(line -> line.contains(" " + HIBERNATE_SQL_LOGGER + " - ")).collect(Collectors.joining("\n"));
	}

	/**
	 * Checks that a tampered request has been rejected before any query and without disclosing credentials
	 */
	@SuppressWarnings("unchecked")
	private void assertRejected(Browser browser, Endpoint endpoint, TableRequest tableRequest) throws Exception {
		Object[] result = sendRecordingSql(browser, endpoint, tableRequest);
		HttpResponse<String> response = (HttpResponse<String>) result[0];
		String body = response.body();
		assertFalse(body.contains(adminPasswordHash), "Password hash disclosed: " + body);
		assertFalse(body.contains(TestDataService.ADMIN_EMAIL), "Rows returned for a rejected request: " + body);
		assertEquals(200, response.statusCode(), body);
		JsonNode json = JSON.readTree(body);
		assertNotNull(json.get("yadaError"), "Not rejected: " + body);
		assertTrue(json.get("data") == null, body);
		assertEquals("", sqlStatements((String) result[1]), "A query ran for a rejected request");
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void legitimateRequestReturnsOnlyConfiguredColumns(Endpoint endpoint) throws Exception {
		Browser browser = admin();
		Object[] result = sendRecordingSql(browser, endpoint, TableRequest.of(endpoint));
		@SuppressWarnings("unchecked")
		String body = ((HttpResponse<String>) result[0]).body();
		assertFalse(sqlStatements((String) result[1]).isEmpty(), "Hibernate SQL is not captured, so the rejection tests would prove nothing");
		JsonNode json = JSON.readTree(body);
		assertTrue(json.get("yadaError") == null, body);
		assertEquals(2, json.get("recordsTotal").asInt(), body);
		JsonNode rows = json.get("data");
		assertEquals(2, rows.size(), body);
		JsonNode first = rows.get(0);
		assertEquals(TestDataService.ADMIN_EMAIL, first.get("userCredentials").get("username").asText(), body);
		assertEquals("admin", first.get("nickname").asText(), body);
		assertEquals(endpoint.tableId + "_UserProfile_" + first.get("id").asText(), first.get("DT_RowId").asText(), body);
		assertFalse(first.get("userCredentials").has("password"), body);
		assertFalse(body.contains(adminPasswordHash), body);
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void extraJsonAttributeForThePasswordIsRejected(Endpoint endpoint) throws Exception {
		TableRequest tableRequest = TableRequest.of(endpoint);
		tableRequest.extraJsonAttributes.add("userCredentials.password");
		assertRejected(admin(), endpoint, tableRequest);
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void columnNameForThePasswordIsRejected(Endpoint endpoint) throws Exception {
		assertRejected(admin(), endpoint, TableRequest.of(endpoint).column(NICKNAME_COLUMN, "nickname", "userCredentials.password"));
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void columnDataForThePasswordIsRejected(Endpoint endpoint) throws Exception {
		assertRejected(admin(), endpoint, TableRequest.of(endpoint).column(NICKNAME_COLUMN, "userCredentials.password", ""));
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void relatedEntityColumnIsRejected(Endpoint endpoint) throws Exception {
		assertRejected(admin(), endpoint, TableRequest.of(endpoint).column(NICKNAME_COLUMN, "userCredentials", "userCredentials"));
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void validButUnconfiguredColumnIsRejected(Endpoint endpoint) throws Exception {
		assertRejected(admin(), endpoint, TableRequest.of(endpoint).column(NICKNAME_COLUMN, "userCredentials.failedAttempts", "userCredentials.failedAttempts"));
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void orderDirectionWithHqlIsRejected(Endpoint endpoint) throws Exception {
		TableRequest tableRequest = TableRequest.of(endpoint);
		tableRequest.orders.set(0, new String[] { String.valueOf(ID_COLUMN), "asc, e.userCredentials.password" });
		assertRejected(admin(), endpoint, tableRequest);
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void searchPathWithHqlIsRejected(Endpoint endpoint) throws Exception {
		TableRequest tableRequest = TableRequest.of(endpoint).column(ID_COLUMN, "id", "id=e.id or e.userCredentials.password");
		tableRequest.searchValue = "a";
		assertRejected(admin(), endpoint, tableRequest);
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void outOfRangeOrderIndexIsRejected(Endpoint endpoint) throws Exception {
		Browser browser = admin();
		for (String index : new String[] { "99", "-1", "x" }) {
			TableRequest tableRequest = TableRequest.of(endpoint);
			tableRequest.orders.set(0, new String[] { index, "asc" });
			assertRejected(browser, endpoint, tableRequest);
		}
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void orderOnAColumnThatIsNotOrderableIsRejected(Endpoint endpoint) throws Exception {
		TableRequest tableRequest = TableRequest.of(endpoint);
		tableRequest.columns.get(LAST_LOGIN_COLUMN)[3] = "true";
		tableRequest.orders.set(0, new String[] { String.valueOf(LAST_LOGIN_COLUMN), "desc" });
		assertRejected(admin(), endpoint, tableRequest);
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void searchFlagOnAColumnThatIsNotSearchableIsIgnored(Endpoint endpoint) throws Exception {
		testDataService.createUser("zz@yts.test", "zebra-password", "zebra", "USER");
		Browser browser = admin();
		TableRequest byEmail = TableRequest.of(endpoint);
		byEmail.searchValue = "zz@yts";
		assertEquals(1, JSON.readTree(browser.table(endpoint, byEmail).body()).get("recordsFiltered").asInt());
		TableRequest byNickname = TableRequest.of(endpoint);
		byNickname.columns.get(NICKNAME_COLUMN)[2] = "true";
		byNickname.searchValue = "zebra";
		String body = browser.table(endpoint, byNickname).body();
		assertEquals(0, JSON.readTree(body).get("recordsFiltered").asInt(), body);
	}

	@ParameterizedTest(name = "{displayName} {0}")
	@EnumSource(Endpoint.class)
	void userWithoutAccessToThePageGetsNoData(Endpoint endpoint) throws Exception {
		// The admin opens the page so that the configured table exists
		admin();
		Browser user = new Browser(TestDataService.USER_EMAIL, TestDataService.USER_PASSWORD);
		HttpResponse<String> response = user.table(endpoint, TableRequest.of(endpoint));
		assertNotEquals(200, response.statusCode(), response.body());
		assertFalse(response.body().contains(TestDataService.ADMIN_EMAIL), response.body());
	}

	@Test
	void subclassColumnOfAConfiguredTableIsOutputOnly() throws Exception {
		testDataService.createUser("zz@yts.test", "zebra-password", "zebra", "USER");
		Browser browser = admin();
		String table = DataTableScenarioController.SUPERCLASS_TABLE_ID;
		try (LogCapture logs = LogCapture.start()) {
			// Second page of one row, sorted by email: the nickname is read from each UserProfile row
			TableRequest page = TableRequest.with(table, "id", "userCredentials.username", "nickname");
			page.orders.set(0, new String[] { "1", "asc" });
			Map<String, String> fields = page.fields();
			fields.put("start", "1");
			fields.put("length", "1");
			String body = browser.post("/yadaDataTableData", fields, browser.csrfHeader, browser.csrfToken).body();
			JsonNode json = JSON.readTree(body);
			assertTrue(json.get("yadaError") == null, body);
			assertEquals(3, json.get("recordsTotal").asInt(), body);
			assertEquals(1, json.get("data").size(), body);
			assertEquals(TestDataService.USER_EMAIL, json.get("data").get(0).get("userCredentials").get("username").asText(), body);
			assertEquals("user", json.get("data").get(0).get("nickname").asText(), body);
			// Searching the nickname is ignored even if the browser asks for it
			TableRequest search = TableRequest.with(table, "id", "userCredentials.username", "nickname");
			search.searchValue = "zebra";
			body = browser.table(Endpoint.CONFIGURED, search).body();
			assertEquals(0, JSON.readTree(body).get("recordsFiltered").asInt(), body);
			long warnings = logs.events().stream().filter(event -> event.getFormattedMessage().contains("DataTable '" + table + "' column 'nickname' is not a persistent field")).count();
			assertEquals(1, warnings, logs.text());
		}
		// Sorting on it is rejected before any query
		TableRequest sort = TableRequest.with(table, "id", "userCredentials.username", "nickname");
		sort.orders.set(0, new String[] { "2", "asc" });
		assertRejected(browser, Endpoint.CONFIGURED, sort);
	}

	@Test
	void localizedMapKeysAreBoundWithTheirType() throws Exception {
		Browser browser = admin();
		TableRequest sorted = TableRequest.with("ytsArticleTable", "id", "sku", "name.en");
		sorted.orders.set(0, new String[] { "2", "desc" });
		String body = browser.post("/test/admin/datatable/articleTablePage", sorted.fields(), browser.csrfHeader, browser.csrfToken).body();
		JsonNode json = JSON.readTree(body);
		assertTrue(json.get("yadaError") == null, body);
		assertEquals(TestDataService.ARTICLES.size(), json.get("data").size(), body);
		assertEquals("Easel", json.get("data").get(0).get("name").get("en").asText(), body);
		TableRequest search = TableRequest.with("ytsArticleTable", "id", "sku", "name.en");
		search.searchValue = "armc";
		body = browser.post("/test/admin/datatable/articleTablePage", search.fields(), browser.csrfHeader, browser.csrfToken).body();
		json = JSON.readTree(body);
		assertEquals(1, json.get("recordsFiltered").asInt(), body);
		assertEquals("Armchair", json.get("data").get(0).get("name").get("en").asText(), body);
	}
}
