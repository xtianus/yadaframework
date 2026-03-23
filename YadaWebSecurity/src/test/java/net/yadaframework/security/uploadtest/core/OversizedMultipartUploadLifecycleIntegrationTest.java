package net.yadaframework.security.uploadtest.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import net.yadaframework.core.YadaEmbeddedTomcatTestServer;
import net.yadaframework.core.YadaTestConfigurationScope;

/**
 * Verifies that embedded-container shutdown does not leak configuration reload threads.
 */
class OversizedMultipartUploadLifecycleIntegrationTest {

	/**
	 * Stops the embedded server and waits for any test-created reloading thread to disappear.
	 * @throws Exception if startup or shutdown fails
	 */
	@Test
	void serverShutdownStopsConfigurationReloadTrigger() throws Exception {
		int baselineReloadingThreads = countReloadingTriggerThreads();
		YadaTestConfigurationScope configurationScope = new YadaTestConfigurationScope(getResourcePath("configuration.xml"));
		try {
			YadaEmbeddedTomcatTestServer server = new YadaEmbeddedTomcatTestServer(getResourcePath("itest-webapp"));
			try {
				server.start();
			} finally {
				server.close();
			}
		} finally {
			configurationScope.close();
		}

		assertTrue(waitUntilReloadingTriggerCountIs(baselineReloadingThreads, Duration.ofSeconds(5)));
	}

	/**
	 * Resolves a test resource to an absolute path.
	 * @param resourcePath the classpath resource to resolve
	 * @return the absolute path of the resource
	 * @throws Exception if the resource cannot be found
	 */
	private Path getResourcePath(String resourcePath) throws Exception {
		URL resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
		assertNotNull(resource, "Missing test resource " + resourcePath);
		return Path.of(resource.toURI());
	}

	/**
	 * Counts the currently alive reload trigger threads.
	 * @return the number of alive reload trigger threads
	 */
	private int countReloadingTriggerThreads() {
		int result = 0;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.isAlive() && thread.getName().startsWith("ReloadingTrigger-")) {
				result++;
			}
		}
		return result;
	}

	/**
	 * Waits until the number of reload trigger threads returns to the expected value.
	 * @param expectedCount the expected number of threads
	 * @param timeout the maximum time to wait
	 * @return true when the expected count is observed before the timeout expires
	 * @throws InterruptedException if the polling sleep is interrupted
	 */
	private boolean waitUntilReloadingTriggerCountIs(int expectedCount, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() <= deadline) {
			if (countReloadingTriggerThreads() == expectedCount) {
				return true;
			}
			Thread.sleep(100);
		}
		return countReloadingTriggerThreads() == expectedCount;
	}
}
