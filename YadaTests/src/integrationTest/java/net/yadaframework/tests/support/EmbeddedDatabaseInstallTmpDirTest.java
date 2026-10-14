package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;
import net.yadaframework.components.YadaMariaDB;

/**
 * The database installation runs with the configured tmpDir, not with the shared system temporary folder.
 * <p>
 * mariadbd deletes every "#sql*" file of its tmpdir when it starts, and mariadb-install-db creates its bootstrap
 * temporary tables (e.g. mysql.tmp_user_sys) there. With a shared folder, an installation that starts while another one
 * is running deletes the other's temporary tables, which then fails with "ERROR: 1051 Unknown table 'mysql.tmp_user_sys'".
 * The test installs without starting the server, so only the installation can have removed the probe file.
 */
@DisabledOnOs(OS.WINDOWS)
class EmbeddedDatabaseInstallTmpDirTest {

	@Test
	void installationUsesTheConfiguredTmpDir() throws Exception {
		try (TestRun run = TestRun.create()) {
			Path tmpDir = run.databaseRoot().resolve("tmp");
			Files.createDirectories(tmpDir);
			// A file that only a mariadbd started with this tmpdir deletes
			Path probe = Files.createFile(tmpDir.resolve("#sql-ytsprobe-1-1.MAI"));
			DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
			configBuilder.setBaseDir(run.databaseBaseDirectory().toFile());
			configBuilder.setDataDir(run.dataDirectory().toFile());
			configBuilder.setTmpDir(tmpDir.toString());
			configBuilder.setPort(run.databasePort());
			DB db = YadaMariaDB.newEmbeddedDB(configBuilder.build());
			assertTrue(Files.isDirectory(run.dataDirectory().resolve("mysql")), "The system tables were not installed");
			assertFalse(Files.exists(probe), "mariadb-install-db did not run with the configured tmpDir: it used the shared system temporary folder");
			assertFalse(TestRun.isListening(run.databasePort()), "The installation must not leave a server running: " + db);
		}
	}

	@Test
	void existingDataFolderIsReusedNotInstalled() throws Exception {
		try (TestRun run = TestRun.create()) {
			Path tmpDir = run.databaseRoot().resolve("tmp");
			Files.createDirectories(tmpDir);
			Path probe = Files.createFile(tmpDir.resolve("#sql-ytsprobe-1-1.MAI"));
			Path existingFile = Files.createDirectories(run.dataDirectory()).resolve("existing.txt");
			Files.writeString(existingFile, "existing data");
			DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
			configBuilder.setBaseDir(run.databaseBaseDirectory().toFile());
			configBuilder.setDataDir(run.dataDirectory().toFile());
			configBuilder.setTmpDir(tmpDir.toString());
			configBuilder.setPort(run.databasePort());
			YadaMariaDB.newEmbeddedDB(configBuilder.build());
			// Like DB.newEmbeddedDB of MariaDB4j 3.3.1: nothing unpacked, nothing installed
			assertFalse(Files.exists(run.databaseBaseDirectory().resolve("scripts")), "The binaries were unpacked over an existing data folder");
			assertFalse(Files.exists(run.dataDirectory().resolve("mysql")), "An existing data folder was installed again");
			assertTrue(Files.exists(existingFile));
			assertTrue(Files.exists(probe), "mariadbd ran although nothing had to be installed");
		}
	}
}
