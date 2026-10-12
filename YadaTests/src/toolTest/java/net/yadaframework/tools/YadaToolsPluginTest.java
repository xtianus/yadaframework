package net.yadaframework.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The YadaTools plugin and its file utilities, applied to in-memory Gradle projects
 */
class YadaToolsPluginTest {

	private static Project project(Path directory) {
		return ProjectBuilder.builder().withProjectDir(directory.toFile()).build();
	}

	@Test
	void pluginNeedsAWebProject(@TempDir Path directory) {
		Project project = project(directory);
		Exception failure = assertThrows(Exception.class, () -> project.getPluginManager().apply(YadaToolsPlugin.class));
		assertTrue(rootMessage(failure).contains("'war' or 'ear'"), rootMessage(failure));
	}

	@Test
	void pluginRegistersTheGenerationAndSchemaTasks(@TempDir Path directory) {
		Project project = project(directory);
		project.getPluginManager().apply("war");
		project.getPluginManager().apply(YadaToolsPlugin.class);
		assertInstanceOf(YadaInitTask.class, project.getTasks().getByName("yadaInit"));
		assertInstanceOf(YadaCreateDbSchemaTask.class, project.getTasks().getByName("yadaDbSchema"));
	}

	@Test
	void copyFileDoesNotOverwriteUnlessAsked(@TempDir Path directory) throws Exception {
		YadaToolsUtil util = new YadaToolsUtil(project(directory));
		File target = directory.resolve("out").toFile();
		util.copyFileFromClasspathFolder("META-INF/robots.txt", target, null, false);
		Path copied = target.toPath().resolve("robots.txt");
		assertTrue(Files.size(copied) > 0);
		Files.writeString(copied, "changed");
		util.copyFileFromClasspathFolder("META-INF/robots.txt", target, null, false);
		assertEquals("changed", Files.readString(copied), "An existing file was overwritten");
		util.copyFileFromClasspathFolder("META-INF/robots.txt", target, "renamed.txt", true);
		util.copyFileFromClasspathFolder("META-INF/robots.txt", target, null, true);
		assertFalse("changed".equals(Files.readString(copied)), "overwrite=true did not replace the file");
		assertTrue(Files.exists(target.toPath().resolve("renamed.txt")));
	}

	@Test
	void missingResourceIsSkipped(@TempDir Path directory) {
		YadaToolsUtil util = new YadaToolsUtil(project(directory));
		File target = directory.resolve("out").toFile();
		util.copyFileFromClasspathFolder("META-INF/does-not-exist.txt", target, null, false);
		assertFalse(target.exists());
	}

	@Test
	void folderInTheJarIsListedAndCopied(@TempDir Path directory) throws Exception {
		YadaToolsUtil util = new YadaToolsUtil(project(directory));
		List<?> emailTemplates = util.listFilesInClasspathFolder("META-INF/email");
		assertTrue(emailTemplates.contains("passwordRecovery.html"), emailTemplates.toString());
		File target = directory.resolve("email").toFile();
		target.mkdirs();
		util.copyAllFromClasspathFolder("META-INF/email", target, false);
		try (var files = Files.list(target.toPath())) {
			assertEquals(emailTemplates.size(), files.count());
		}
	}

	private static String rootMessage(Throwable throwable) {
		StringBuilder result = new StringBuilder();
		for (Throwable current = throwable; current != null; current = current.getCause()) {
			result.append(current.getMessage()).append(" | ");
		}
		return result.toString();
	}
}
