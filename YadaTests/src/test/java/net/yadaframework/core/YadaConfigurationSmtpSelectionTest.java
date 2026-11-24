package net.yadaframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;
import java.util.Properties;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the SMTP configuration section is selected according to the environment and to the
 * "enabled" attribute of the optional &lt;smtpserver-mailpit> section, and that all email getters
 * read from the same selected section.
 */
class YadaConfigurationSmtpSelectionTest {

	private static final String REAL_HOST = "smtp.provider.example";
	private static final String MAILPIT_HOST = "localhost";

	/**
	 * Builds a configuration with the standard smtp section and, optionally, a mailpit section.
	 * @param environment the value of config/info/env, like "dev" or "prod"
	 * @param mailpitEnabledAttribute the literal value of the "enabled" attribute, or null for no mailpit section at all
	 */
	private YadaConfiguration buildConfiguration(String environment, String mailpitEnabledAttribute) throws Exception {
		StringBuilder xml = new StringBuilder();
		xml.append("<combined><config>");
		xml.append("<info><env>").append(environment).append("</env></info>");
		xml.append("<email>");
		xml.append("<smtpserver>");
		xml.append("<host>").append(REAL_HOST).append("</host>");
		xml.append("<port>587</port>");
		xml.append("<protocol>smtps</protocol>");
		xml.append("<username>realuser</username>");
		xml.append("<password>realpassword</password>");
		xml.append("<properties>mail.smtp.auth=true</properties>");
			xml.append("<properties>mail.smtp.sendpartial=true</properties>");
		xml.append("</smtpserver>");
		if (mailpitEnabledAttribute != null) {
			xml.append("<smtpserver-mailpit enabled=\"").append(mailpitEnabledAttribute).append("\">");
			xml.append("<host>").append(MAILPIT_HOST).append("</host>");
			xml.append("<port>1025</port>");
			xml.append("<protocol>smtp</protocol>");
			xml.append("<username></username>");
			xml.append("<password></password>");
			xml.append("<properties>mail.smtp.auth=false</properties>");
				xml.append("<properties>mail.smtp.sendpartial=true</properties>");
			xml.append("</smtpserver-mailpit>");
		}
		xml.append("</email>");
		xml.append("</config></combined>");

		XMLConfiguration xmlConfiguration = new XMLConfiguration();
		xmlConfiguration.setExpressionEngine(new XPathExpressionEngine());
		new org.apache.commons.configuration2.io.FileHandler(xmlConfiguration).load(new StringReader(xml.toString()));

		YadaConfiguration yadaConfiguration = new YadaConfiguration() {};
		yadaConfiguration.setConfiguration(xmlConfiguration);
		return yadaConfiguration;
	}

	/** All six getters must read from the same section. */
	private void assertAllGettersUseRealServer(YadaConfiguration config) {
		assertEquals(REAL_HOST, config.getEmailHost());
		assertEquals(587, config.getEmailPort());
		assertEquals("smtps", config.getEmailProtocol());
		assertEquals("realuser", config.getEmailUsername());
		assertEquals("realpassword", config.getEmailPassword());
		Properties properties = config.getEmailProperties();
		assertEquals("true", properties.getProperty("mail.smtp.auth"));
	}

	private void assertAllGettersUseMailpit(YadaConfiguration config) {
		assertEquals(MAILPIT_HOST, config.getEmailHost());
		assertEquals(1025, config.getEmailPort());
		assertEquals("smtp", config.getEmailProtocol());
		assertEquals("", config.getEmailUsername());
		assertEquals("", config.getEmailPassword());
		Properties properties = config.getEmailProperties();
		assertEquals("false", properties.getProperty("mail.smtp.auth"));
	}

	@Test
	void mailpitSectionAbsentUsesTheRealServer() throws Exception {
		assertAllGettersUseRealServer(buildConfiguration("dev", null));
	}

	@Test
	void unresolvedVariableUsesTheRealServer() throws Exception {
		// Commons Configuration leaves an unresolved variable in place as literal text
		assertAllGettersUseRealServer(buildConfiguration("dev", "${usemailpit}"));
	}

	@Test
	void falseValueUsesTheRealServer() throws Exception {
		assertAllGettersUseRealServer(buildConfiguration("dev", "false"));
	}

	@Test
	void trueValueUsesMailpitInDevelopment() throws Exception {
		assertAllGettersUseMailpit(buildConfiguration("dev", "true"));
	}

	@Test
	void mixedCaseValueUsesMailpitInDevelopment() throws Exception {
		assertAllGettersUseMailpit(buildConfiguration("dev", "TRUE"));
		assertAllGettersUseMailpit(buildConfiguration("dev", "True"));
	}

	@Test
	void collaudoEnvironmentIgnoresTheFlag() throws Exception {
		assertAllGettersUseRealServer(buildConfiguration("col", "true"));
	}

	@Test
	void productionEnvironmentIgnoresTheFlag() throws Exception {
		assertAllGettersUseRealServer(buildConfiguration("prod", "true"));
	}

	@Test
	void missingStandardSectionReturnsNullWithoutThrowing() throws Exception {
		XMLConfiguration xmlConfiguration = new XMLConfiguration();
		xmlConfiguration.setExpressionEngine(new XPathExpressionEngine());
		new org.apache.commons.configuration2.io.FileHandler(xmlConfiguration)
			.load(new StringReader("<combined><config><info><env>dev</env></info><email/></config></combined>"));
		YadaConfiguration config = new YadaConfiguration() {};
		config.setConfiguration(xmlConfiguration);
		assertNull(config.getEmailHost());
		assertEquals(0, config.getEmailPort());
	}
}
