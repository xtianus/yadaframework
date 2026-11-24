package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;

import net.yadaframework.components.YadaMariaDBServer;
import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaJpaConfig;
import net.yadaframework.core.YadaTestConfigurationScope;
import net.yadaframework.exceptions.YadaSystemException;

/**
 * Native embedded MariaDB lifecycle: fresh install, reopen, ownership, attachment and startup failures.
 */
class EmbeddedDatabaseLifecycleTest {
	private final List<YadaMariaDBServer> started = new ArrayList<>();
	private final List<TestRun> runs = new ArrayList<>();

	@AfterEach
	void cleanup() {
		// Every resource gets its cleanup attempt, even after a failure
		List<Runnable> steps = new ArrayList<>();
		for (YadaMariaDBServer server : started) {
			steps.add(server::stopOwnedDatabase);
		}
		steps.add(OwnedDatabases::stopAll);
		for (TestRun run : runs) {
			steps.add(run::close);
		}
		IllegalStateException failure = null;
		for (Runnable step : steps) {
			try {
				step.run();
			} catch (RuntimeException e) {
				if (failure == null) {
					failure = new IllegalStateException("Cleanup failed");
				}
				failure.addSuppressed(e);
			}
		}
		if (failure != null) {
			throw failure;
		}
	}

	private TestRun newRun() {
		TestRun run = TestRun.create();
		runs.add(run);
		return run;
	}

	private YadaMariaDBServer startServer(YadaConfiguration config) {
		YadaMariaDBServer server = new YadaMariaDBServer(config);
		started.add(server);
		return server;
	}

	private static String queryString(String jdbcUrl, String sql) throws SQLException {
		try (Connection connection = DriverManager.getConnection(jdbcUrl, "root", ""); Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
			assertTrue(resultSet.next(), "No result for " + sql);
			return resultSet.getString(1);
		}
	}

	private static boolean schemaExists(TestRun run, String schemaName) throws SQLException {
		return "1".equals(queryString(run.serverJdbcUrl(), "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='" + schemaName + "'"));
	}

	@Test
	void freshDirectoryStartsNativeMariaDB() throws Exception {
		TestRun run = newRun();
		assertFalse(TestRun.isListening(run.databasePort()));
		YadaMariaDBServer server = startServer(run.loadConfiguration());
		assertEquals(run.databasePort(), server.getPort());
		assertTrue(queryString(run.serverJdbcUrl(), "SELECT VERSION()").contains("MariaDB"));
		assertTrue(schemaExists(run, run.schemaName()));
		assertTrue(Files.list(run.databaseBaseDirectory()).findAny().isPresent(), "Binaries not installed in the run folder");
		server.stopOwnedDatabase();
		assertFalse(TestRun.isListening(run.databasePort()), "Owned database still running after stop");
	}

	@Test
	void restartAfterGracefulStopReopensExistingData() throws Exception {
		TestRun run = newRun();
		YadaMariaDBServer first = startServer(run.loadConfiguration());
		try (Connection connection = DriverManager.getConnection(run.configuredJdbcUrl(), "root", ""); Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE survivor (id INT PRIMARY KEY)");
			statement.executeUpdate("INSERT INTO survivor VALUES (42)");
		}
		first.stopOwnedDatabase();
		assertFalse(TestRun.isListening(run.databasePort()));
		// The binaries folder is not empty now, so the YadaMariaDB reopen path is used
		YadaMariaDBServer second = startServer(run.loadConfiguration());
		assertEquals("42", queryString(run.configuredJdbcUrl(), "SELECT id FROM survivor"));
		second.stopOwnedDatabase();
		assertFalse(TestRun.isListening(run.databasePort()));
	}

	@Test
	void stoppingTwiceIsHarmless() throws Exception {
		TestRun run = newRun();
		YadaMariaDBServer server = startServer(run.loadConfiguration());
		server.stopOwnedDatabase();
		server.stopOwnedDatabase();
		assertFalse(TestRun.isListening(run.databasePort()));
	}

	@Test
	void attachedInstanceCannotStopTheOwnersProcess() throws Exception {
		TestRun run = newRun();
		YadaMariaDBServer owner = startServer(run.loadConfiguration());
		YadaMariaDBServer attached = startServer(run.loadConfiguration());
		attached.stopOwnedDatabase();
		assertTrue(TestRun.isListening(run.databasePort()), "The attached instance stopped a process it does not own");
		assertTrue(queryString(run.serverJdbcUrl(), "SELECT VERSION()").contains("MariaDB"));
		owner.stopOwnedDatabase();
		assertFalse(TestRun.isListening(run.databasePort()));
	}

	@Test
	void attachmentToServerWithDifferentDataDirectoryFails() throws Exception {
		TestRun ownerRun = newRun();
		YadaMariaDBServer owner = startServer(ownerRun.loadConfiguration());
		TestRun intruderRun = newRun();
		intruderRun.setDatabasePort(ownerRun.databasePort());
		// A non-empty binaries folder selects the reopen-or-attach path
		Files.createDirectories(intruderRun.databaseBaseDirectory());
		Files.writeString(intruderRun.databaseBaseDirectory().resolve("placeholder"), "not empty");
		YadaSystemException failure = assertThrows(YadaSystemException.class, () -> new YadaMariaDBServer(intruderRun.loadConfiguration()));
		assertTrue(rootMessage(failure).contains("data directory"), rootMessage(failure));
		// Nothing was created in, and nothing stopped, the foreign server
		assertTrue(TestRun.isListening(ownerRun.databasePort()));
		assertFalse(schemaExists(ownerRun, intruderRun.schemaName()));
		assertFalse(Files.exists(intruderRun.dataDirectory()) && Files.list(intruderRun.dataDirectory()).findAny().isPresent(), "Files written in the intruder data directory");
		owner.stopOwnedDatabase();
		intruderRun.setDatabasePort(TestRun.findFreePort());
	}

	@Test
	void failureAfterLaunchStopsTheOwnedDatabase() throws Exception {
		TestRun run = newRun();
		// Without a datasource url the schema creation fails after the process has been launched
		run.setConfiguredJdbcUrl(null);
		assertThrows(YadaSystemException.class, () -> new YadaMariaDBServer(run.loadConfiguration()));
		assertFalse(TestRun.isListening(run.databasePort()), "A failed constructor left its database running");
	}

	@Test
	void failedSpringStartupStillStopsTheOwnedDatabase() throws Exception {
		TestRun run = newRun();
		// Invalid pool settings make the datasource fail after the embedded database has started.
		// A value that is not a number would be taken as "no datasource configured" and fall back to JNDI instead.
		run.setDatasourceSetting("poolInitialSize", "5");
		run.setDatasourceSetting("poolMaxSize", "1");
		try (YadaTestConfigurationScope scope = new YadaTestConfigurationScope(run.configurationFile())) {
			AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
			new EmbeddedDatabaseOwnership().initialize(context);
			context.register(LifecycleAppConfig.class, YadaJpaConfig.class);
			Exception failure = assertThrows(Exception.class, context::refresh);
			assertTrue(isPoolValidationFailure(failure), "The refresh failed for an unexpected reason: " + rootMessage(failure));
			assertEquals(1, OwnedDatabases.registered().size(), "The database was not captured before the failure");
			assertTrue(TestRun.isListening(run.databasePort()), "The database should have started before the failure");
			OwnedDatabases.stopAll();
			assertFalse(TestRun.isListening(run.databasePort()));
		}
	}

	@Test
	void concurrentRunsAreIsolated() throws Exception {
		TestRun first = newRun();
		TestRun second = newRun();
		assertNotEquals(first.directory(), second.directory());
		assertNotEquals(first.databasePort(), second.databasePort());
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			List<Callable<YadaMariaDBServer>> startups = List.of(() -> new YadaMariaDBServer(first.loadConfiguration()), () -> new YadaMariaDBServer(second.loadConfiguration()));
			// Collect every successful startup before reporting a failure, so that cleanup can stop them all
			ExecutionException startupFailure = null;
			for (Future<YadaMariaDBServer> future : executor.invokeAll(startups)) {
				try {
					started.add(future.get());
				} catch (ExecutionException e) {
					startupFailure = e;
				}
			}
			if (startupFailure != null) {
				throw startupFailure;
			}
		} finally {
			executor.shutdownNow();
		}
		assertTrue(queryString(first.serverJdbcUrl(), "SELECT @@datadir").startsWith(first.dataDirectory().toString()));
		assertTrue(queryString(second.serverJdbcUrl(), "SELECT @@datadir").startsWith(second.dataDirectory().toString()));
		assertFalse(schemaExists(first, second.schemaName()));
		assertFalse(schemaExists(second, first.schemaName()));
	}

	/**
	 * @return true when the failure comes from the Vibur pool rejecting its configuration
	 */
	private static boolean isPoolValidationFailure(Throwable throwable) {
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			StackTraceElement[] stackTrace = current.getStackTrace();
			if (current instanceof IllegalArgumentException && stackTrace.length > 0 && stackTrace[0].getClassName().startsWith("org.vibur.")) {
				return true;
			}
		}
		return false;
	}

	private static String rootMessage(Throwable throwable) {
		StringBuilder result = new StringBuilder();
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			result.append(current.getMessage()).append(" | ");
		}
		return result.toString();
	}

	/**
	 * Minimal root configuration: the static run configuration as the "config" bean.
	 */
	static class LifecycleAppConfig extends YadaAppConfig {
		@Bean
		public YadaConfiguration config() {
			return getStaticConfig();
		}
	}
}
