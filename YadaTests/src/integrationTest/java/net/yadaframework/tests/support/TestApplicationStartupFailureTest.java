package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * An application whose schema is missing or broken must fail startup visibly and release everything it started.
 */
class TestApplicationStartupFailureTest {
	private TestRun run;

	@BeforeEach
	void createRun() {
		run = TestRun.create();
	}

	@AfterEach
	void cleanup() {
		OwnedDatabases.stopAll();
		run.close();
	}

	private void assertStartupFails(String expectedMessagePart) {
		RuntimeException failure = assertThrows(RuntimeException.class, () -> TestApplication.start(run).close());
		assertTrue(failure.getMessage().contains("failed to start"), failure.getMessage());
		assertTrue(causeMessages(failure).contains(expectedMessagePart), "Unexpected cause: " + causeMessages(failure));
		assertTrue(OwnedDatabases.registered().isEmpty(), "The failed application left a database registered");
		assertFalse(TestRun.isListening(run.databasePort()), "The failed application left its database running");
		assertTrue(run.directory().toFile().isDirectory());
	}

	private static String causeMessages(Throwable throwable) {
		StringBuilder result = new StringBuilder();
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			result.append(current.getMessage()).append(" | ");
		}
		return result.toString();
	}

	@Test
	void missingSchemaFailsStartup() {
		run.setMigrationAtStartup(false);
		assertStartupFails("has not been migrated");
	}

	@Test
	void brokenMigrationFailsStartup() throws Exception {
		Path migrations = Files.createDirectories(run.directory().resolve("brokenMigrations"));
		Files.writeString(migrations.resolve("V002__broken.sql"), "CREATE TABLE broken (id INT PRIMARY KEY;\n", StandardCharsets.UTF_8);
		run.setExtraConfiguration("<yts><extraMigrationLocation>filesystem:" + migrations + "</extraMigrationLocation></yts>");
		assertStartupFails("V002");
	}
}
