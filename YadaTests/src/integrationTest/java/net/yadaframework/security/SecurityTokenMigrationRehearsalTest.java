package net.yadaframework.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.tests.components.TestDataService;
import net.yadaframework.tests.support.ApplicationSession;

/**
 * Upgrade rehearsal of V002__hashed_security_tokens.sql: a separate schema of the test database server is migrated to
 * V001 with the Flyway settings of YadaAppConfig, filled with legacy token rows, then migrated to the latest version.
 * Every legacy token is gone and the token columns have their new shape, while user credentials, roles and profiles
 * survive.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecurityTokenMigrationRehearsalTest {
	private ApplicationSession session;
	private String schema;

	@BeforeAll
	void startApplication() throws SQLException {
		session = ApplicationSession.start(application -> application.getBean(TestDataService.class)::seedDefaults);
		schema = session.run().schemaName() + "_upgrade";
		try (Connection connection = DriverManager.getConnection(session.run().serverJdbcUrl(), "root", ""); Statement statement = connection.createStatement()) {
			statement.execute("CREATE DATABASE `" + schema + "`");
		}
	}

	@AfterAll
	void stopApplication() throws SQLException {
		if (session != null) {
			try (Connection connection = DriverManager.getConnection(session.run().serverJdbcUrl(), "root", ""); Statement statement = connection.createStatement()) {
				statement.execute("DROP DATABASE IF EXISTS `" + schema + "`");
			}
			session.close();
		}
	}

	private String schemaUrl() {
		return session.run().serverJdbcUrl().replace("/?", "/" + schema + "?");
	}

	/**
	 * Flyway configured like YadaAppConfig does at startup, on the rehearsal schema
	 */
	private Flyway flyway(String target) {
		YadaConfiguration config = session.application().getBean(YadaConfiguration.class);
		return Flyway.configure()
			.dataSource(schemaUrl(), "root", "")
			.cleanDisabled(true)
			.locations("classpath:database")
			.baselineOnMigrate(true)
			.table(config.flywayTableName())
			.target(target)
			.load();
	}

	private static long count(Statement statement, String sql) throws SQLException {
		try (ResultSet resultSet = statement.executeQuery(sql)) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

	private Set<String> columns(Statement statement, String table) throws SQLException {
		Set<String> result = new HashSet<>();
		try (ResultSet resultSet = statement.executeQuery("SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='" + schema + "' AND TABLE_NAME='" + table + "'")) {
			while (resultSet.next()) {
				result.add(resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
			}
		}
		return result;
	}

	@Test
	void legacyTokensAreInvalidatedAndAccountsSurvive() throws SQLException {
		flyway("1").migrate();
		try (Connection connection = DriverManager.getConnection(schemaUrl(), "root", ""); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO YadaUserCredentials (id, changePassword, enabled, failedAttempts, version, password, username) VALUES (1, 0, 1, 0, 0, 'hash-1', 'legacy@yts.test'), (2, 0, 1, 0, 0, 'hash-2', 'other@yts.test')");
			statement.executeUpdate("INSERT INTO YadaUserCredentials_roles (roles, YadaUserCredentials_id) VALUES (2, 1), (8, 1), (2, 2)");
			statement.executeUpdate("INSERT INTO YadaUserProfile (id, timezoneSetByUser, userCredentials_id, version, DTYPE, nickname) VALUES (1, 0, 1, 0, 'UserProfile', 'legacy'), (2, 0, 2, 0, 'UserProfile', 'other')");
			statement.executeUpdate("INSERT INTO YadaAutoLoginToken (id, expiration, timestamp, token, version, yadaUserCredentials_id) VALUES (1, NULL, NOW(), 6542972110427423233, 0, 1), (2, NOW() + INTERVAL 1 DAY, NOW(), 576947321597250461, 0, 2)");
			statement.executeUpdate("INSERT INTO YadaRegistrationRequest (id, registrationType, timestamp, token, version, yadaUserCredentials_id, email, password) VALUES "
				+ "(1, 0, NOW(), 791072808031857248, 0, NULL, 'new@yts.test', 'clear'), (2, 1, NOW(), 2633579417400869106, 0, NULL, 'legacy@yts.test', 'fakefake'), (3, 2, NOW(), 3477474374499055961, 0, 1, 'changed@yts.test', 'unused')");
		}

		flyway("latest").migrate();

		try (Connection connection = DriverManager.getConnection(schemaUrl(), "root", ""); Statement statement = connection.createStatement()) {
			assertEquals(0, count(statement, "SELECT COUNT(*) FROM YadaAutoLoginToken"));
			assertEquals(0, count(statement, "SELECT COUNT(*) FROM YadaRegistrationRequest"));
			assertEquals(2, count(statement, "SELECT COUNT(*) FROM YadaUserCredentials WHERE (id=1 AND username='legacy@yts.test' AND password='hash-1') OR (id=2 AND username='other@yts.test')"));
			assertEquals(3, count(statement, "SELECT COUNT(*) FROM YadaUserCredentials_roles"));
			assertEquals(2, count(statement, "SELECT COUNT(*) FROM YadaUserProfile WHERE nickname IN ('legacy', 'other')"));

			Set<String> autoLoginColumns = columns(statement, "YadaAutoLoginToken");
			assertTrue(autoLoginColumns.contains("tokenHash varchar(64) NO"), autoLoginColumns.toString());
			assertFalse(autoLoginColumns.stream().anyMatch(column -> column.startsWith("token ")), autoLoginColumns.toString());
			Set<String> requestColumns = columns(statement, "YadaRegistrationRequest");
			assertTrue(requestColumns.contains("tokenHash varchar(64) NO"), requestColumns.toString());
			assertTrue(requestColumns.contains("expiration timestamp YES"), requestColumns.toString());
			assertFalse(requestColumns.stream().anyMatch(column -> column.startsWith("token ")), requestColumns.toString());
			assertEquals(1, count(statement, "SELECT COUNT(*) FROM " + session.application().getBean(YadaConfiguration.class).flywayTableName() + " WHERE version='002' AND success=1"));
		}
	}
}
