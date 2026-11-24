package net.yadaframework.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.StringReader;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.junit.jupiter.api.Test;

/**
 * The Hibernate dialect chosen by YadaJpaConfig from &lt;database>&lt;dialect> and the embedded database setting
 */
class YadaConfigurationDialectTest {

	private YadaConfiguration buildConfiguration(String databaseContent) throws Exception {
		String xml = "<combined><config><database>" + databaseContent + "</database></config></combined>";
		XMLConfiguration xmlConfiguration = new XMLConfiguration();
		xmlConfiguration.setExpressionEngine(new XPathExpressionEngine());
		new FileHandler(xmlConfiguration).load(new StringReader(xml));
		YadaConfiguration config = new YadaConfiguration() {};
		config.setConfiguration(xmlConfiguration);
		return config;
	}

	@Test
	void withoutConfigurationHibernateDetectsTheDialect() throws Exception {
		YadaConfiguration config = buildConfiguration("");
		assertNull(config.getDbDialect());
		assertNull(YadaJpaConfig.chooseDialect(config), "No dialect must be forced on a normal database");
	}

	@Test
	void theEmbeddedDatabaseUsesMariaDB() throws Exception {
		YadaConfiguration config = buildConfiguration("<embedded enabled=\"true\"/>");
		assertEquals("org.hibernate.dialect.MariaDBDialect", YadaJpaConfig.chooseDialect(config));
	}

	@Test
	void theConfiguredDialectWins() throws Exception {
		YadaConfiguration server = buildConfiguration("<dialect>org.hibernate.dialect.MariaDBDialect</dialect>");
		assertEquals("org.hibernate.dialect.MariaDBDialect", YadaJpaConfig.chooseDialect(server));
		YadaConfiguration embedded = buildConfiguration("<dialect>org.hibernate.dialect.MySQLDialect</dialect><embedded enabled=\"true\"/>");
		assertEquals("org.hibernate.dialect.MySQLDialect", YadaJpaConfig.chooseDialect(embedded));
	}
}
