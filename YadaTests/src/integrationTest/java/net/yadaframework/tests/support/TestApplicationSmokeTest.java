package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.yadaframework.cms.persistence.entity.YadaArticle;
import net.yadaframework.commerce.persistence.entity.YadaCart;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.tests.components.StartupSchemaCheck;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.persistence.entity.UserProfile;

/**
 * The YadaTests application on native embedded MariaDB: migration, rendering, security, persistence and fixtures.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestApplicationSmokeTest {
	private TestRun run;
	private TestApplication application;
	private DatabaseFixtures fixtures;
	private Object previousApplicationContext;

	@BeforeAll
	void startApplication() {
		previousApplicationContext = YadaUtil.applicationContext;
		run = TestRun.create();
		try {
			application = TestApplication.start(run);
		} catch (RuntimeException e) {
			run.close();
			throw e;
		}
		fixtures = new DatabaseFixtures(application, application.getBean(TestDataService.class)::seedDefaults);
		fixtures.reset();
	}

	@AfterAll
	void stopApplication() {
		try {
			if (application != null) {
				application.close();
			}
		} finally {
			run.close();
		}
		assertTrue(OwnedDatabases.registered().isEmpty());
		assertSame(previousApplicationContext, YadaUtil.applicationContext, "A closed context is still reachable from YadaUtil");
	}

	private static HttpClient newClient() {
		CookieManager cookieManager = new CookieManager();
		cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		return HttpClient.newBuilder().cookieHandler(cookieManager).followRedirects(HttpClient.Redirect.NEVER).build();
	}

	private HttpResponse<String> get(HttpClient client, String path) throws Exception {
		return client.send(HttpRequest.newBuilder(application.resolve(path)).GET().build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private HttpResponse<String> postForm(HttpClient client, String path, String form) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(application.resolve(path)).header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(form)).build();
		return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
	}

	private List<String> queryColumn(String sql) throws SQLException {
		List<String> result = new ArrayList<>();
		try (Connection connection = DriverManager.getConnection(run.configuredJdbcUrl(), "root", ""); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			while (resultSet.next()) {
				result.add(resultSet.getString(1));
			}
		}
		return result;
	}

	private List<String> schemaFingerprint() throws SQLException {
		return queryColumn("SELECT CONCAT_WS(':', TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='" + run.schemaName() + "' ORDER BY TABLE_NAME, COLUMN_NAME");
	}

	@Test
	@Order(1)
	void baselineMigrationRanAsSql() throws Exception {
		List<String> history = queryColumn("SELECT CONCAT_WS(':', version, type, script, success) FROM flyway_schema_history ORDER BY installed_rank");
		assertEquals(List.of("001:SQL:" + StartupSchemaCheck.BASELINE_SCRIPT + ":1"), history, "An automatic baseline row does not prove that V001 ran");
		assertTrue(queryColumn("SHOW TABLES").contains("YadaUserProfile"));
	}

	@Test
	@Order(2)
	void rendersARealPageWithCsrfToken() throws Exception {
		HttpResponse<String> response = get(newClient(), "/test/home");
		assertEquals(200, response.statusCode(), response.body());
		Document document = Jsoup.parse(response.body());
		assertEquals("YadaTests home", document.selectFirst("[data-testid=title]").text());
		assertFalse(document.selectFirst("meta[name=_csrf]").attr("content").isBlank());
		// The default data has been read through the application DAOs and rendered by Thymeleaf
		assertEquals("admin", document.selectFirst("[data-testid=adminNickname]").text());
	}

	@Test
	@Order(3)
	void csrfTokenIsRequiredAndAccepted() throws Exception {
		HttpClient client = newClient();
		assertEquals(403, postForm(client, "/test/echo", "message=hello").statusCode());
		Document document = Jsoup.parse(get(client, "/test/home").body());
		String token = document.selectFirst("meta[name=_csrf]").attr("content");
		HttpResponse<String> accepted = postForm(client, "/test/echo", "message=hello&_csrf=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
		assertEquals(200, accepted.statusCode(), accepted.body());
		assertEquals("ECHO:hello", accepted.body());
	}

	@Test
	@Order(4)
	void userFixtureIsPersistedAndReadBack() throws Exception {
		TestDataService testDataService = application.getBean(TestDataService.class);
		// Test code and the application share the same classes
		assertSame(TestDataService.class, testDataService.getClass());
		UserProfile created = testDataService.createUser("reader@yts.test", "reader-password", "reader", "USER");
		assertNotNull(created.getId());
		UserProfile found = testDataService.findUser("reader@yts.test");
		assertEquals(created.getId(), found.getId());
		assertEquals("reader", found.getNickname());
		assertEquals(List.of("reader"), queryColumn("SELECT nickname FROM YadaUserProfile WHERE id=" + created.getId()));
	}

	@Test
	@Order(5)
	void resetRemovesCommittedDataAndReseedsDeterministically() throws Exception {
		TestDataService testDataService = application.getBean(TestDataService.class);
		Long adminId = testDataService.findUser(TestDataService.ADMIN_EMAIL).getId();
		testDataService.createUser("extra@yts.test", "extra-password", "extra", "USER");
		EntityManager entityManager = application.getBean(EntityManagerFactory.class).createEntityManager();
		try {
			entityManager.getTransaction().begin();
			entityManager.persist(new YadaArticle());
			entityManager.persist(new YadaCart());
			entityManager.getTransaction().commit();
		} finally {
			entityManager.close();
		}
		assertEquals(TestDataService.ARTICLES.size() + 1, fixtures.countRows("YadaArticle"));
		assertTrue(fixtures.countRows("YadaCart") > 0);
		fixtures.reset();
		assertEquals(TestDataService.ARTICLES.size(), fixtures.countRows("YadaArticle"));
		assertEquals(0, fixtures.countRows("YadaCart"));
		assertEquals(2, fixtures.countRows("YadaUserProfile"));
		assertEquals(2, fixtures.countRows("YadaUserCredentials"));
		assertNull(testDataService.findUser("extra@yts.test"));
		assertEquals(adminId, testDataService.findUser(TestDataService.ADMIN_EMAIL).getId(), "Reseeded ids are not deterministic");
		assertEquals(1, fixtures.countRows("flyway_schema_history"), "The reset removed the migration history");
	}

	@Test
	@Order(6)
	void schemaIsUnchangedAfterRestart() throws Exception {
		List<String> before = schemaFingerprint();
		application.close();
		assertFalse(TestRun.isListening(run.databasePort()), "The owned database is still running after close");
		application = TestApplication.start(run);
		fixtures = new DatabaseFixtures(application, application.getBean(TestDataService.class)::seedDefaults);
		assertEquals(before, schemaFingerprint());
		assertEquals(1, fixtures.countRows("flyway_schema_history"));
		assertEquals(200, get(newClient(), "/test/home").statusCode());
	}
}
