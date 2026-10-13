package net.yadaframework.tests.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.commons.configuration2.ImmutableConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Per-run settings of {@link TestRun#setConfigurationValue(String, String...)} override the application configuration
 * (conf.webapp.yts.xml) and the run configuration without changing the other values. No database is started.
 */
class TestRunSettingsTest {
	private TestRun run;

	@BeforeEach
	void createRun() {
		run = TestRun.create();
	}

	@AfterEach
	void deleteRun() {
		run.close();
	}

	private ImmutableConfiguration configuration() {
		return run.loadConfiguration().getConfiguration();
	}

	@Test
	void defaultsComeFromTheApplicationAndRunConfiguration() {
		ImmutableConfiguration configuration = configuration();
		assertEquals(48, configuration.getInt("config/security/autologinExpirationHours"));
		assertEquals(240, configuration.getInt("config/security/sessionTimeoutMinutes"));
		assertTrue(configuration.getBoolean("config/email/enabled"), "Mail must reach the capturing sender");
		assertEquals("dev", configuration.getString("config/info/env"));
	}

	@Test
	void singleValueOverridesOnlyThatValue() {
		run.setConfigurationValue("security/autologinExpirationHours", "1");
		run.setConfigurationValue("paths/serverAddress", "http://proxy.example/");
		ImmutableConfiguration configuration = configuration();
		assertEquals(1, configuration.getInt("config/security/autologinExpirationHours"));
		assertEquals(240, configuration.getInt("config/security/sessionTimeoutMinutes"), "Sibling application settings are kept");
		assertEquals("http://proxy.example/", configuration.getString("config/paths/serverAddress"), "Run settings are overridden");
		assertTrue(configuration.getString("config/paths/basePath").contains(run.runId()), "Sibling run settings are kept");
		assertEquals(run.databasePort(), configuration.getInt("config/database/embedded/@port"));
	}

	@Test
	void repeatedValuesReplaceTheWholeList() {
		run.setConfigurationValue("security/trustedProxies/proxy", "127.0.0.1", "::1");
		run.setConfigurationValue("i18n/locale", "it", "en");
		ImmutableConfiguration configuration = configuration();
		assertEquals(List.of("127.0.0.1", "::1"), configuration.getList(String.class, "config/security/trustedProxies/proxy"));
		assertEquals(List.of("it", "en"), configuration.getList(String.class, "config/i18n/locale"));
	}

	/**
	 * The base lists have several entries: the two roles of conf.webapp.yts.xml and three proxies in the run configuration
	 */
	@Test
	void repeatedValuesReplaceABaseListWithSeveralEntries() {
		assertEquals(List.of("ADMIN", "USER"), configuration().getList(String.class, "config/security/roles/role/key"));
		run.setExtraConfiguration("<yts><proxies><proxy>10.0.0.1</proxy><proxy>10.0.0.2</proxy><proxy>10.0.0.3</proxy></proxies></yts>");
		assertEquals(List.of("10.0.0.1", "10.0.0.2", "10.0.0.3"), configuration().getList(String.class, "config/yts/proxies/proxy"));

		run.setConfigurationValue("yts/proxies/proxy", "192.168.1.1");
		assertEquals(List.of("192.168.1.1"), configuration().getList(String.class, "config/yts/proxies/proxy"));
		run.setConfigurationValue("yts/proxies/proxy", "192.168.1.1", "192.168.1.2");
		assertEquals(List.of("192.168.1.1", "192.168.1.2"), configuration().getList(String.class, "config/yts/proxies/proxy"));

		run.setConfigurationValue("security/roles/role/key", "ONLY");
		ImmutableConfiguration configuration = configuration();
		assertEquals(List.of("ONLY"), configuration.getList(String.class, "config/security/roles/role/key"), "The configured roles were merged with the setting");
		assertFalse(configuration.containsKey("config/security/roles/role/id"), "The ids of the configured roles survived");
		assertEquals(240, configuration.getInt("config/security/sessionTimeoutMinutes"), "Siblings of the replaced list are kept");
	}

	@Test
	void attributesAndRemoval() {
		run.setConfigurationValue("email/@throwExceptions", "true");
		run.setConfigurationValue("security/autologinExpirationHours", "2");
		assertTrue(configuration().getBoolean("config/email/@throwExceptions"));
		run.setConfigurationValue("security/autologinExpirationHours");
		assertEquals(48, configuration().getInt("config/security/autologinExpirationHours"));
		run.clearConfigurationValues();
		assertEquals(false, configuration().getBoolean("config/email/@throwExceptions", false));
	}

	@Test
	void valuesAreEscaped() {
		run.setConfigurationValue("yts/text", "<a href=\"x\">&</a>");
		assertEquals("<a href=\"x\">&</a>", configuration().getString("config/yts/text"));
	}

	@Test
	void invalidPathsAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> run.setConfigurationValue("/security/x", "1"));
		assertThrows(IllegalArgumentException.class, () -> run.setConfigurationValue("security//x", "1"));
		assertThrows(IllegalArgumentException.class, () -> run.setConfigurationValue("security/@a/b", "1"));
		assertThrows(IllegalArgumentException.class, () -> run.setConfigurationValue("security/<x>", "1"));
		assertThrows(IllegalArgumentException.class, () -> run.setConfigurationValue("email/@throwExceptions", "a", "b"));
	}
}
