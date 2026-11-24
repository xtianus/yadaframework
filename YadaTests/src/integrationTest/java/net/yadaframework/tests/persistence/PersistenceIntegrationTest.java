package net.yadaframework.tests.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import net.yadaframework.cms.persistence.entity.YadaArticle;
import net.yadaframework.commerce.persistence.entity.YadaOrderStatus;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.persistence.YadaMoney;
import net.yadaframework.persistence.YadaSavedPropertyCodecs;
import net.yadaframework.persistence.YadaSavedPropertyKey;
import net.yadaframework.persistence.repository.YadaPersistentEnumDao;
import net.yadaframework.persistence.repository.YadaSavedPropertyDao;
import net.yadaframework.tests.components.CapturingMailSender;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.persistence.entity.UserProfile;
import net.yadaframework.tests.persistence.repository.ArticleDao;
import net.yadaframework.tests.persistence.repository.UserProfileDao;
import net.yadaframework.tests.support.ApplicationSession;
import net.yadaframework.tests.support.TestRun;
import net.yadaframework.web.YadaPageRequest;
import net.yadaframework.web.YadaPageRows;

/**
 * Framework persistence and configuration against the real MariaDB schema of the application: no database mock.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistenceIntegrationTest {
	private ApplicationSession session;

	@BeforeAll
	void start() {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
	}

	@AfterAll
	void stop() {
		if (session != null) {
			session.close();
		}
	}

	@BeforeEach
	void reset() {
		session.fixtures().reset();
	}

	private <T> T bean(Class<T> type) {
		return session.application().getBean(type);
	}

	private void executeSql(String sql) throws SQLException {
		TestRun run = session.run();
		try (Connection connection = DriverManager.getConnection(run.configuredJdbcUrl(), "root", ""); Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}

	@Test
	void daoInsertReadUpdateDelete() {
		TestDataService testDataService = bean(TestDataService.class);
		UserProfileDao userProfileDao = bean(UserProfileDao.class);
		UserProfile created = testDataService.createUser("crud@yts.test", "crud-password", "before", "USER");
		UserProfile read = userProfileDao.find(created.getId());
		assertEquals("before", read.getNickname());
		read.setNickname("after");
		userProfileDao.save(read);
		assertEquals("after", userProfileDao.findUserProfileByUsername("crud@yts.test").getNickname());
		// Credentials and roles hang off the profile: the delete must go through their foreign keys
		TransactionTemplate transaction = new TransactionTemplate(bean(PlatformTransactionManager.class));
		// The "entityManager" bean of YadaJpaConfig is not bound to Spring transactions: use a shared one
		EntityManager entityManager = SharedEntityManagerCreator.createSharedEntityManager(bean(EntityManagerFactory.class));
		transaction.executeWithoutResult(status -> entityManager.remove(entityManager.find(UserProfile.class, created.getId())));
		assertNull(userProfileDao.findUserProfileByUsername("crud@yts.test"));
	}

	@Test
	void rolledBackTransactionLeavesNoRow() {
		ArticleDao articleDao = bean(ArticleDao.class);
		long before = session.fixtures().countRows("YadaArticle");
		TransactionTemplate transaction = new TransactionTemplate(bean(PlatformTransactionManager.class));
		assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
			articleDao.createCommerceArticle("SKU-ROLLBACK", Locale.ENGLISH, "Rolled back", new YadaMoney(100));
			throw new IllegalStateException("Rollback on purpose");
		}));
		assertEquals(before, session.fixtures().countRows("YadaArticle"));
	}

	@Test
	void foreignKeysProtectReferencedRows() {
		UserProfile user = bean(TestDataService.class).findUser(TestDataService.USER_EMAIL);
		Long credentialsId = user.getUserCredentials().getId();
		SQLException failure = assertThrows(SQLException.class, () -> executeSql("DELETE FROM YadaUserCredentials WHERE id=" + credentialsId));
		assertTrue(failure instanceof SQLIntegrityConstraintViolationException || failure.getMessage().contains("foreign key"), failure.getMessage());
		assertNotNull(bean(TestDataService.class).findUser(TestDataService.USER_EMAIL));
	}

	@Test
	void savedPropertiesAreUniquePerApplicationAndName() throws Exception {
		YadaSavedPropertyDao yadaSavedPropertyDao = bean(YadaSavedPropertyDao.class);
		YadaSavedPropertyKey<BigDecimal> key = YadaSavedPropertyKey.of("Shop", "VatRate", YadaSavedPropertyCodecs.decimalCodec());
		yadaSavedPropertyDao.setValue(key, new BigDecimal("22.00"));
		// The same normalized key updates the existing row
		yadaSavedPropertyDao.setValue(YadaSavedPropertyKey.of(" shop ", "vatrate", YadaSavedPropertyCodecs.decimalCodec()), new BigDecimal("10.5"));
		assertEquals(0, new BigDecimal("10.5").compareTo(yadaSavedPropertyDao.getValue(key)));
		assertEquals(1, session.fixtures().countRows("YadaSavedProperty"));
		// The database refuses a second row for the same key, whatever the caller does
		SQLException duplicate = assertThrows(SQLException.class, () -> executeSql("INSERT INTO YadaSavedProperty (version, applicationName, name, type, value) VALUES (0, 'shop', 'vatrate', 'DECIMAL', '1')"));
		assertTrue(duplicate instanceof SQLIntegrityConstraintViolationException || duplicate.getMessage().contains("Duplicate"), duplicate.getMessage());
		assertTrue(yadaSavedPropertyDao.delete(key));
		assertFalse(yadaSavedPropertyDao.findValue(key).isPresent());
	}

	@Test
	void persistentEnumsAreStoredAndLocalized() {
		YadaPersistentEnumDao yadaPersistentEnumDao = bean(YadaPersistentEnumDao.class);
		assertEquals(YadaOrderStatus.values().length, session.fixtures().countRows("YadaPersistentEnum"));
		assertEquals("Paid", yadaPersistentEnumDao.find(YadaOrderStatus.PAID).getLangToText().get("en"));
	}

	@Test
	void pagedAndSortedQueriesThroughYadaPageRequest() {
		ArticleDao articleDao = bean(ArticleDao.class);
		YadaPageRequest firstPage = new YadaPageRequest(0, 2);
		firstPage.appendSort("sku").desc();
		YadaPageRows<YadaArticle> rows = articleDao.find(firstPage);
		assertEquals(2, rows.getNumberOfElements());
		assertEquals("SKU-E", rows.getContent().get(0).getSku());
		assertEquals("SKU-D", rows.getContent().get(1).getSku());
		assertFalse(rows.isLast());
		YadaPageRows<YadaArticle> lastPage = articleDao.find(new YadaPageRequest(2, 2));
		assertEquals(1, lastPage.getNumberOfElements());
		assertTrue(lastPage.isLast());
	}

	@Test
	void configurationComesFromTheApplicationAndRunFiles() {
		YadaConfiguration config = bean(YadaConfiguration.class);
		assertEquals(8, config.getRoleId("ADMIN"));
		assertEquals(102400, config.getMaxFileUploadSizeBytes());
		assertTrue(config.isUseEmbeddedDatabase());
		assertEquals(session.run().databasePort(), config.getEmbeddedDatabasePort());
		// Email is enabled, but only towards the in-memory CapturingMailSender
		assertTrue(config.isEmailEnabled());
		assertTrue(bean(JavaMailSender.class) instanceof CapturingMailSender);
		assertEquals(Locale.ENGLISH, config.getDefaultLocale());
	}

	@Test
	void frameworkResourcesAreServed() throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		// Templates reach this folder through yada:src/yada:href, which add the framework version
		String yadaResources = bean(YadaConfiguration.class).getVersionedYadaResourceDir();
		HttpResponse<String> yadaJs = client.send(HttpRequest.newBuilder(session.application().resolve(yadaResources + "/js/yada.js")).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, yadaJs.statusCode());
		assertTrue(yadaJs.body().contains("yada"), "Not the yada.js script");
		HttpResponse<String> jquery = client.send(HttpRequest.newBuilder(session.application().resolve("/static/jquery-3.7.1.min.js")).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(200, jquery.statusCode());
		HttpResponse<String> missing = client.send(HttpRequest.newBuilder(session.application().resolve("/static/missing.js")).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
		assertEquals(404, missing.statusCode());
	}

	@Test
	void embeddedDatabaseGetsTheMariaDBDialectWithoutConfiguration() {
		assertNull(bean(YadaConfiguration.class).getDbDialect(), "The application must not need a <dialect>");
		Dialect dialect = bean(EntityManagerFactory.class).unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
		assertTrue(dialect instanceof MariaDBDialect, dialect.getClass().getName());
	}

	@Test
	void entityManagerFactoryUsesTheEmbeddedDatabase() {
		Object url = bean(EntityManagerFactory.class).getProperties().get("hibernate.connection.url");
		if (url != null) {
			assertTrue(url.toString().contains(":" + session.run().databasePort() + "/"), url.toString());
		}
		assertEquals(session.run().schemaName(), bean(EntityManager.class).createNativeQuery("SELECT DATABASE()").getSingleResult());
	}
}
