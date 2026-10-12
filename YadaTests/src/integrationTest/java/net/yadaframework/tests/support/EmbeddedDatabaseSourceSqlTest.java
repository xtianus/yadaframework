package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.yadaframework.components.YadaMariaDBServer;

/**
 * The disposable &lt;sourceSql> seed of the embedded database. It is kept apart from the Flyway migrations of the application.
 */
class EmbeddedDatabaseSourceSqlTest {
	private TestRun run;
	private YadaMariaDBServer server;

	@BeforeEach
	void createRun() {
		run = TestRun.create();
		// The application migrations are not part of this suite
		run.setMigrationAtStartup(false);
	}

	@AfterEach
	void cleanup() {
		try {
			if (server != null) {
				server.stopOwnedDatabase();
			}
		} finally {
			run.close();
		}
	}

	/**
	 * Copies a classpath sql template into the run directory, replacing the schema placeholder.
	 */
	private Path copySeed(String resourceName) throws Exception {
		try (InputStream inputStream = getClass().getResourceAsStream("/sourceSql/" + resourceName)) {
			String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			Path copy = run.directory().resolve("seed copy.sql");
			Files.writeString(copy, template.replace("@SCHEMA@", run.schemaName()), StandardCharsets.UTF_8);
			return copy;
		}
	}

	private long countRows(String table) throws SQLException {
		try (Connection connection = DriverManager.getConnection(run.configuredJdbcUrl(), "root", ""); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
			resultSet.next();
			return resultSet.getLong(1);
		}
	}

	private boolean tableExists(String table) throws SQLException {
		try (Connection connection = DriverManager.getConnection(run.serverJdbcUrl(), "root", ""); Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + run.schemaName() + "' AND TABLE_NAME='" + table + "'")) {
			resultSet.next();
			return resultSet.getLong(1) == 1;
		}
	}

	@Test
	void firstCreationAppliesTheSeedAndRenamesOnlyTheCopy() throws Exception {
		Path seed = copySeed("seed.sql");
		run.setSourceSql(seed);
		server = new YadaMariaDBServer(run.loadConfiguration());
		assertEquals(2, countRows("seeded_item"));
		assertFalse(Files.exists(seed));
		assertTrue(Files.exists(Path.of(seed + ".loaded")));
		assertTrue(getClass().getResource("/sourceSql/seed.sql") != null, "The original seed must never be renamed");
	}

	@Test
	void reopeningDoesNotReplayTheSeed() throws Exception {
		Path seed = copySeed("seed.sql");
		run.setSourceSql(seed);
		server = new YadaMariaDBServer(run.loadConfiguration());
		server.stopOwnedDatabase();
		// Put the seed back so that only the existing schema can prevent a replay
		Files.copy(Path.of(seed + ".loaded"), seed);
		server = new YadaMariaDBServer(run.loadConfiguration());
		assertEquals(2, countRows("seeded_item"));
		assertTrue(Files.exists(seed), "The seed was loaded again");
	}

	@Test
	void invalidSeedIsOnlyLoggedSoTheDataMustBeChecked() throws Exception {
		Path seed = copySeed("invalid.sql");
		run.setSourceSql(seed);
		server = new YadaMariaDBServer(run.loadConfiguration());
		// The component starts anyway: only explicit schema/data assertions reveal the failure
		assertTrue(TestRun.isListening(run.databasePort()));
		assertFalse(tableExists("broken_item"));
		assertTrue(Files.exists(seed), "A seed that failed to load must not be marked as loaded");
	}

	@Test
	void unreadableSeedIsIgnored() throws Exception {
		run.setSourceSql(run.directory().resolve("missing.sql"));
		server = new YadaMariaDBServer(run.loadConfiguration());
		assertTrue(TestRun.isListening(run.databasePort()));
		assertFalse(tableExists("seeded_item"));
	}
}
