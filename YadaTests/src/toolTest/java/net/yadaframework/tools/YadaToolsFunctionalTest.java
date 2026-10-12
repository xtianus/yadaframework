package net.yadaframework.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The YadaTools plugin in real Gradle builds, run with TestKit in temporary project folders.
 * Only the generation tasks run: never deployment, SSH or publishing tasks.
 */
class YadaToolsFunctionalTest {

	private static List<File> pluginClasspath() {
		String classpath = System.getProperty("yts.toolsPluginClasspath");
		if (classpath == null || classpath.isBlank()) {
			throw new IllegalStateException("The yts.toolsPluginClasspath system property is missing: run through the toolTest task");
		}
		return Arrays.stream(classpath.split(File.pathSeparator)).map(File::new).toList();
	}

	private static GradleRunner runner(Path projectDir, String... arguments) {
		GradleRunner runner = GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath(pluginClasspath()).withArguments(arguments).forwardOutput();
		String testKitDir = System.getProperty("yts.testKitDir");
		if (testKitDir != null) {
			runner.withTestKitDir(new File(testKitDir));
		}
		return runner;
	}

	private static void writeBuild(Path projectDir, String initConfiguration) throws Exception {
		Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'yttest'\n");
		Files.writeString(projectDir.resolve("build.gradle"), """
			plugins {
				id 'war'
				id 'net.yadaframework.tools'
			}
			yadaInit {
			%s
			}
			""".formatted(initConfiguration));
	}

	@Test
	void yadaInitGeneratesTheApplicationSkeleton(@TempDir Path projectDir) throws Exception {
		writeBuild(projectDir, """
				projectName = 'YtTest'
				acronym = 'ytt'
				basePackage = 'com.example.ytt'
				basePath = '%s'
			""".formatted(projectDir.resolve("srv").toString().replace("\\", "/")));
		BuildResult result = runner(projectDir, "yadaInit", "--stacktrace").build();
		assertEquals(TaskOutcome.SUCCESS, result.task(":yadaInit").getOutcome());
		for (String expected : List.of("src/main/webapp/WEB-INF/web.xml", "src/main/webapp/WEB-INF/messages/messages.properties",
				"src/main/webapp/WEB-INF/views/home.html", "src/main/resources/configuration.xml", "src/main/resources/META-INF/persistence.xml",
				"src/main/webapp/res/js/ytt.js", "src/main/java/com/example/ytt/core")) {
			assertTrue(Files.exists(projectDir.resolve(expected)), "Not generated: " + expected);
		}
		// Templates are processed with the task properties
		String persistence = Files.readString(projectDir.resolve("src/main/resources/META-INF/persistence.xml"));
		assertTrue(persistence.contains("com.example.ytt"), persistence);
		for (String env : List.of("dev", "tst", "prod")) {
			assertTrue(Files.isDirectory(projectDir.resolve("env").resolve(env)), "No env folder " + env);
		}
	}

	@Test
	void yadaInitRunsAgainWithoutOverwritingChanges(@TempDir Path projectDir) throws Exception {
		writeBuild(projectDir, """
				projectName = 'YtTest'
				acronym = 'ytt'
				basePackage = 'com.example.ytt'
				basePath = '%s'
			""".formatted(projectDir.resolve("srv").toString().replace("\\", "/")));
		runner(projectDir, "yadaInit").build();
		Path webXml = projectDir.resolve("src/main/webapp/WEB-INF/web.xml");
		Files.writeString(webXml, "<!-- edited -->");
		runner(projectDir, "yadaInit").build();
		assertEquals("<!-- edited -->", Files.readString(webXml));
	}

	@Test
	void yadaInitWithoutProjectNameFails(@TempDir Path projectDir) throws Exception {
		writeBuild(projectDir, "");
		BuildResult result = runner(projectDir, "yadaInit").buildAndFail();
		assertTrue(result.getOutput().contains("'projectName' task property undefined"), result.getOutput());
	}

	@Test
	void pluginWithoutWarFailsTheBuild(@TempDir Path projectDir) throws Exception {
		Files.writeString(projectDir.resolve("settings.gradle"), "rootProject.name = 'yttest'\n");
		Files.writeString(projectDir.resolve("build.gradle"), "plugins { id 'net.yadaframework.tools' }\n");
		BuildResult result = runner(projectDir, "tasks").buildAndFail();
		assertTrue(result.getOutput().contains("'war' or 'ear'"), result.getOutput());
	}

	@Test
	void schemaTaskIsAvailable(@TempDir Path projectDir) throws Exception {
		writeBuild(projectDir, "");
		BuildResult result = runner(projectDir, "tasks", "--all").build();
		assertTrue(result.getOutput().contains("yadaDbSchema"), result.getOutput());
		assertTrue(result.getOutput().contains("yadaInit"), result.getOutput());
	}
}
