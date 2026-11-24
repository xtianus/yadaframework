package net.yadaframework.tests.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import net.yadaframework.tests.support.OwnedDatabases;
import net.yadaframework.tests.support.TestRun;

/**
 * A failing browser assertion still releases the browser, the application and its native database,
 * and leaves a screenshot and a trace behind.
 */
class CleanupAfterFailureE2eTest {

	private static List<ProcessHandle> browserProcesses() {
		return ProcessHandle.current().descendants().filter(ProcessHandle::isAlive)
			.filter(process -> process.info().command().map(command -> command.contains("chrom") || command.contains("playwright") || command.endsWith("/node")).orElse(false))
			.toList();
	}

	private static Stream<Path> scenarioArtifactFolders() throws Exception {
		Path root = BrowserTestSupport.artifactsDirectory();
		if (!Files.isDirectory(root)) {
			return Stream.empty();
		}
		return Files.list(root).filter(folder -> folder.getFileName().toString().startsWith(DeliberatelyFailingScenario.class.getSimpleName() + "-"));
	}

	@Test
	void failingBrowserTestReleasesEverything() throws Exception {
		// Artifacts of earlier runs must not count
		try (Stream<Path> stale = scenarioArtifactFolders()) {
			for (Path folder : stale.toList()) {
				try (Stream<Path> files = Files.walk(folder)) {
					files.sorted(Comparator.reverseOrder()).forEach(file -> file.toFile().delete());
				}
			}
		}
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass(DeliberatelyFailingScenario.class)).build();
		Launcher launcher = LauncherFactory.create();
		SummaryGeneratingListener listener = new SummaryGeneratingListener();
		launcher.execute(request, listener);
		TestExecutionSummary summary = listener.getSummary();

		assertEquals(1, summary.getTestsFoundCount());
		assertEquals(1, summary.getTestsFailedCount(), "The scenario should fail on its assertion");
		assertEquals(0, summary.getContainersFailedCount(), "Setup or cleanup of the failing class failed: " + summary.getFailures());
		List<Path> folders;
		try (Stream<Path> found = scenarioArtifactFolders()) {
			folders = found.toList();
		}
		assertEquals(1, folders.size(), "Expected one artifact folder: " + folders);
		Path artifacts = folders.get(0);
		assertTrue(Files.isRegularFile(artifacts.resolve("screenshot.png")), "No screenshot in " + artifacts);
		assertTrue(Files.isRegularFile(artifacts.resolve("trace.zip")), "No trace in " + artifacts);
		assertTrue(DeliberatelyFailingScenario.lastDatabasePort > 0);
		assertFalse(TestRun.isListening(DeliberatelyFailingScenario.lastDatabasePort), "The database of the failed class is still running");
		assertTrue(OwnedDatabases.registered().isEmpty());
		assertEquals(List.of(), browserProcesses(), "Browser processes left behind");
	}
}
