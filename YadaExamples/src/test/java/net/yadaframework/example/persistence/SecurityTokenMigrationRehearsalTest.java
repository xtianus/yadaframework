package net.yadaframework.example.persistence;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.yadaframework.example.support.ExampleTestDatabase;
import net.yadaframework.example.support.ExampleWebTestBase;

/**
 * Upgrade rehearsal of the example V002 migration, which downstream applications copy: a separate schema of the example
 * embedded database is migrated to V001 like the application does at startup, filled with legacy token rows (also of
 * the YexRegistrationRequest subclass), then migrated to the latest version. Every legacy token is gone and the token
 * columns have their new shape, while credentials, roles and profiles survive.
 */
class SecurityTokenMigrationRehearsalTest extends ExampleWebTestBase {
	private static final String SCHEMA = "yexdbtest_upgrade";

	private static String serverUrl(String schema) {
		return "jdbc:mysql://localhost:" + ExampleTestDatabase.databasePort() + "/" + schema + "?useSSL=false&allowPublicKeyRetrieval=true";
	}

	@BeforeEach
	void createSchema() throws SQLException {
		try (Connection connection = DriverManager.getConnection(serverUrl(""), "root", ""); Statement statement = connection.createStatement()) {
			statement.execute("DROP DATABASE IF EXISTS `" + SCHEMA + "`");
			statement.execute("CREATE DATABASE `" + SCHEMA + "`");
		}
	}

	@AfterEach
	void dropSchema() throws SQLException {
		try (Connection connection = DriverManager.getConnection(serverUrl(""), "root", ""); Statement statement = connection.createStatement()) {
			statement.execute("DROP DATABASE IF EXISTS `" + SCHEMA + "`");
		}
	}

	/**
	 * Flyway configured like YadaAppConfig does at startup
	 */
	private Flyway flyway(String target) {
		return Flyway.configure()
			.dataSource(serverUrl(SCHEMA), "root", "")
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

	private static Set<String> columns(Statement statement, String table) throws SQLException {
		Set<String> result = new HashSet<>();
		try (ResultSet resultSet = statement.executeQuery("SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='" + SCHEMA + "' AND TABLE_NAME='" + table + "'")) {
			while (resultSet.next()) {
				result.add(resultSet.getString(1) + " " + resultSet.getString(2) + " " + resultSet.getString(3));
			}
		}
		return result;
	}

	@Test
	void legacyTokensAreInvalidatedAndAccountsSurvive() throws SQLException {
		flyway("1").migrate();
		try (Connection connection = DriverManager.getConnection(serverUrl(SCHEMA), "root", ""); Statement statement = connection.createStatement()) {
			statement.executeUpdate("INSERT INTO YadaUserCredentials (id, changePassword, enabled, failedAttempts, version, password, username) VALUES (1, 0, 1, 0, 0, 'hash-1', 'legacy@example.test'), (2, 0, 1, 0, 0, 'hash-2', 'other@example.test')");
			statement.executeUpdate("INSERT INTO YadaUserCredentials_roles (roles, YadaUserCredentials_id) VALUES (2, 1), (8, 1), (2, 2)");
			statement.executeUpdate("INSERT INTO YadaUserProfile (id, timezoneSetByUser, userCredentials_id, version, DTYPE, firstName, nickname) VALUES (1, 0, 1, 0, 'UserProfile', 'Legacy', 'legacy'), (2, 0, 2, 0, 'UserProfile', 'Other', 'other')");
			statement.executeUpdate("INSERT INTO YadaAutoLoginToken (id, expiration, timestamp, token, version, yadaUserCredentials_id) VALUES (1, NULL, NOW(), 6542972110427423233, 0, 1), (2, NOW() + INTERVAL 1 DAY, NOW(), 576947321597250461, 0, 2)");
			statement.executeUpdate("INSERT INTO YadaRegistrationRequest (id, registrationType, timestamp, token, version, yadaUserCredentials_id, email, password) VALUES "
				+ "(1, 0, NOW(), 791072808031857248, 0, NULL, 'new@example.test', 'clear'), (2, 1, NOW(), 2633579417400869106, 0, NULL, 'legacy@example.test', 'fakefake'), (3, 2, NOW(), 3477474374499055961, 0, 1, 'changed@example.test', 'unused')");
			statement.executeUpdate("INSERT INTO YexRegistrationRequest (id, name, surname) VALUES (1, 'New', 'Comer')");
		}

		flyway("latest").migrate();

		try (Connection connection = DriverManager.getConnection(serverUrl(SCHEMA), "root", ""); Statement statement = connection.createStatement()) {
			assertEquals(0, count(statement, "SELECT COUNT(*) FROM YadaAutoLoginToken"));
			assertEquals(0, count(statement, "SELECT COUNT(*) FROM YadaRegistrationRequest"));
			assertEquals(0, count(statement, "SELECT COUNT(*) FROM YexRegistrationRequest"));
			assertEquals(2, count(statement, "SELECT COUNT(*) FROM YadaUserCredentials WHERE (id=1 AND username='legacy@example.test' AND password='hash-1') OR (id=2 AND username='other@example.test')"));
			assertEquals(3, count(statement, "SELECT COUNT(*) FROM YadaUserCredentials_roles"));
			assertEquals(2, count(statement, "SELECT COUNT(*) FROM YadaUserProfile WHERE firstName IN ('Legacy', 'Other')"));

			Set<String> autoLoginColumns = columns(statement, "YadaAutoLoginToken");
			assertTrue(autoLoginColumns.contains("tokenHash varchar(64) NO"), autoLoginColumns.toString());
			assertFalse(autoLoginColumns.stream().anyMatch(column -> column.startsWith("token ")), autoLoginColumns.toString());
			Set<String> requestColumns = columns(statement, "YadaRegistrationRequest");
			assertTrue(requestColumns.contains("tokenHash varchar(64) NO"), requestColumns.toString());
			assertTrue(requestColumns.contains("expiration timestamp YES"), requestColumns.toString());
			assertFalse(requestColumns.stream().anyMatch(column -> column.startsWith("token ")), requestColumns.toString());
			// A user can hold several autologin tokens and registration requests (ManyToOne)
			assertEquals(0, count(statement, "SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='" + SCHEMA + "' AND TABLE_NAME IN ('YadaAutoLoginToken', 'YadaRegistrationRequest') AND COLUMN_NAME='yadaUserCredentials_id' AND NON_UNIQUE=0"));
			statement.executeUpdate("INSERT INTO YadaAutoLoginToken (expiration, timestamp, tokenHash, version, yadaUserCredentials_id) VALUES (NOW() + INTERVAL 1 HOUR, NOW(), '" + "a".repeat(64) + "', 0, 1), (NOW() + INTERVAL 1 HOUR, NOW(), '" + "b".repeat(64) + "', 0, 1)");
		}
	}
}
