package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import net.yadaframework.components.YadaUtil;
import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.core.YadaEmbeddedTomcatTestServer;
import net.yadaframework.core.YadaTestConfigurationScope;

/**
 * Two applications with different configurations started and closed one after the other in the same JVM
 * don't see each other's static configuration, application context or reload threads.
 */
class ConfigurationIsolationTest {

	private static Path resourcePath(String resource) throws Exception {
		URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
		assertNotNull(url, "Missing test resource " + resource);
		return Path.of(url.toURI());
	}

	private static int countReloadingTriggerThreads() {
		int result = 0;
		for (Thread thread : Thread.getAllStackTraces().keySet()) {
			if (thread.isAlive() && thread.getName().startsWith("ReloadingTrigger-")) {
				result++;
			}
		}
		return result;
	}

	private static boolean waitForReloadingTriggerCount(int expected, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (countReloadingTriggerThreads() != expected && System.nanoTime() < deadline) {
			Thread.sleep(100);
		}
		return countReloadingTriggerThreads() == expected;
	}

	/**
	 * The static configuration that a component would see while the scope is open
	 */
	private static YadaConfiguration staticConfiguration() {
		return YadaAppConfig.getStaticConfig();
	}

	@Test
	void startCloseStartAcrossTwoConfigurations() throws Exception {
		int baselineThreads = countReloadingTriggerThreads();
		String previousConfigurationFile = System.getProperty("yada.configurationFile");
		Object previousApplicationContext = YadaUtil.applicationContext;

		// First application: upload test configuration, database disabled, reloading enabled
		try (YadaTestConfigurationScope scope = new YadaTestConfigurationScope(resourcePath("uploadtest-configuration.xml"));
			YadaEmbeddedTomcatTestServer server = new YadaEmbeddedTomcatTestServer(resourcePath("uploadtest-webapp"))) {
			server.start();
			assertFalse(staticConfiguration().isDatabaseEnabled());
			assertEquals(1, staticConfiguration().getMaxFileUploadSizeBytes());
		}
		assertTrue(waitForReloadingTriggerCount(baselineThreads, Duration.ofSeconds(5)), "A reload thread of the first application survived");
		assertEquals(previousConfigurationFile, System.getProperty("yada.configurationFile"));

		// Second application: the YadaTests application on its own run
		TestRun run = TestRun.create();
		try {
			try (TestApplication application = TestApplication.start(run)) {
				YadaConfiguration config = application.getBean(YadaConfiguration.class);
				assertTrue(config.isDatabaseEnabled());
				// 1 in the upload test configuration, 102400 in conf.webapp.yts.xml
				assertEquals(102400, config.getMaxFileUploadSizeBytes(), "The upload test configuration leaked into the next application");
				assertSame(config, staticConfiguration());
			}
		} finally {
			run.close();
		}
		assertTrue(waitForReloadingTriggerCount(baselineThreads, Duration.ofSeconds(5)), "A reload thread of the second application survived");
		assertEquals(previousConfigurationFile, System.getProperty("yada.configurationFile"));
		assertSame(previousApplicationContext, YadaUtil.applicationContext, "A closed context is still reachable from YadaUtil");
	}
}
