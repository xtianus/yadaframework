package net.yadaframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import ch.vorburger.exec.ManagedProcessBuilder;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

/**
 * Guards the copy of the MariaDB4j install command in {@link YadaMariaDB}: a MariaDB4j upgrade that changes its
 * arguments must be reviewed, and --tmpdir must stay.
 */
@DisabledOnOs(OS.WINDOWS) // Windows runs the original MariaDB4j command
class YadaMariaDBInstallCommandTest {

	@Test
	void installCommandHasTheMariaDB4jArgumentsPlusTmpDir(@TempDir Path folder) throws Exception {
		File baseDir = folder.resolve("MariaDB").toFile();
		File dataDir = folder.resolve("data").toFile();
		File tmpDir = folder.resolve("tmp").toFile();
		// MariaDB4j only looks the tool up: an empty file is enough
		Path installTool = Files.createDirectories(folder.resolve("MariaDB/bin")).resolve("mariadb-install-db");
		Files.createFile(installTool);
		DBConfigurationBuilder configBuilder = DBConfigurationBuilder.newBuilder();
		configBuilder.setBaseDir(baseDir);
		configBuilder.setDataDir(dataDir);
		configBuilder.setTmpDir(tmpDir.getPath());
		configBuilder.setPort(3307);
		YadaMariaDB db = new YadaMariaDB(configBuilder.build());

		ManagedProcessBuilder builder = db.createUnixInstallProcessBuilder();

		assertEquals(List.of(
			"--datadir=" + dataDir.getCanonicalPath(),
			"--basedir=" + baseDir.getCanonicalPath(),
			"--no-defaults",
			"--force",
			"--skip-name-resolve",
			"--tmpdir=" + tmpDir.getCanonicalPath()), builder.getArguments());
		assertEquals(baseDir, builder.getWorkingDirectory());
		assertEquals(installTool.toFile().getPath(), builder.getExecutable());
	}
}
