package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import ch.vorburger.exec.ManagedProcessException;
import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import net.yadaframework.components.YadaMariaDBServer;
import net.yadaframework.exceptions.YadaSystemException;

/**
 * Guards the whitespace check of {@link YadaMariaDBServer} (checkSupportedPaths), which exists only because of a MariaDB4j bug
 * on Linux and macOS: folder arguments are passed as --datadir="/some path" and the quotes reach MariaDB literally.
 * <p>
 * The test passes while the bug exists. It fails, explaining what to do, as soon as MariaDB4j can use such a folder.
 */
@DisabledOnOs(OS.WINDOWS)
class EmbeddedDatabaseWhitespacePathTest {
	private static final String BUG_FIXED_MESSAGE = """

		MariaDB4j CAN NOW INSTALL AND START MARIADB IN A FOLDER CONTAINING WHITESPACE.

		Reason of this failure:
		YadaMariaDBServer.checkSupportedPaths() refuses embedded database folders containing whitespace on non-Windows systems,
		because MariaDB4j used to pass folder arguments as --datadir="/some path": no shell is involved, so the quotes reached
		mariadb-install-db and mariadbd literally and the installation failed with "FATAL ERROR: Could not find my_print_defaults".
		This test has just installed and started MariaDB through MariaDB4j directly in such a folder, and it worked:
		the MariaDB4j version currently on the classpath has fixed the bug, so the check is no longer needed.

		How to fix:
		1. Remove YadaMariaDBServer.checkSupportedPaths() and its call in the YadaMariaDBServer constructor (YadaWeb).
		2. Delete this test class (YadaTests/src/integrationTest/.../EmbeddedDatabaseWhitespacePathTest.java).
		3. Remove the whitespace limitation from YadaDocs/src/docs/asciidoc/en/database/embedded.adoc.
		4. Optionally add a lifecycle test that starts the embedded database in a run directory containing spaces.
		""";

	@Test
	void whitespaceFoldersAreRejectedWhileMariaDB4jCantUseThem() throws Exception {
		TestRun run = TestRun.create(TestRun.buildDirectory().resolve("runs with spaces"));
		try {
			// 1. Yada refuses the folder with a clear message, before installing anything
			YadaSystemException failure = assertThrows(YadaSystemException.class, () -> new YadaMariaDBServer(run.loadConfiguration()));
			assertTrue(fullMessage(failure).contains("contains whitespace"), fullMessage(failure));
			assertFalse(Files.exists(run.databaseBaseDirectory()), "Binaries were unpacked despite the check");
			assertFalse(Files.exists(run.dataDirectory()), "The data folder was created despite the check");
			assertFalse(TestRun.isListening(run.databasePort()));
			// 2. MariaDB4j itself still can't use the folder, otherwise the check must go
			assertMariaDB4jStillFails(run);
		} finally {
			run.close();
		}
	}

	private void assertMariaDB4jStillFails(TestRun run) {
		Path root = run.databaseRoot();
		DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
		configBuilder.setBaseDir(root.resolve("MariaDB").toFile());
		configBuilder.setDataDir(root.resolve("data").toFile());
		configBuilder.setTmpDir(root.resolve("tmp").toString());
		configBuilder.setPort(run.databasePort());
		DB db = null;
		try {
			db = DB.newEmbeddedDB(configBuilder.build());
			db.start();
		} catch (ManagedProcessException expected) {
			if (fullMessage(expected).contains("my_print_defaults")) {
				// The MariaDB4j bug is still there: the check is needed
				return;
			}
			// Any other failure says nothing about the bug
			throw new AssertionError("MariaDB4j failed for an unexpected reason, so the whitespace bug can't be verified", expected);
		} finally {
			if (db != null) {
				try {
					db.stop();
				} catch (ManagedProcessException e) {
					// Reported by run.close() if the port is still in use
				}
			}
		}
		fail(BUG_FIXED_MESSAGE);
	}

	private static String fullMessage(Throwable throwable) {
		StringBuilder result = new StringBuilder();
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			result.append(current.getMessage()).append(" | ");
		}
		return result.toString();
	}
}
