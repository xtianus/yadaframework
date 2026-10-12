package net.yadaframework.tests.components;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import net.yadaframework.core.YadaConfiguration;

/**
 * Fails the root context startup when the baseline migration has not been applied, so that an application
 * with a missing or broken schema never serves pages.
 */
@Component
public class StartupSchemaCheck {
	private final Logger log = LoggerFactory.getLogger(getClass());
	public static final String BASELINE_SCRIPT = "V001__baseline.sql";

	private final DataSource dataSource;
	private final YadaConfiguration config;

	public StartupSchemaCheck(DataSource dataSource, YadaConfiguration config) {
		this.dataSource = dataSource;
		this.config = config;
	}

	@EventListener
	public void verifyBaseline(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() != null) {
			return; // Only the root context owns the database
		}
		String table = config.flywayTableName();
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		Integer applied;
		try {
			applied = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE script=? AND type='SQL' AND success=1", Integer.class, BASELINE_SCRIPT);
		} catch (RuntimeException e) {
			throw new IllegalStateException("The database schema has not been migrated: " + table + " can't be read", e);
		}
		if (applied == null || applied != 1) {
			throw new IllegalStateException("The baseline migration " + BASELINE_SCRIPT + " has not been applied");
		}
		log.debug("Baseline migration {} verified", BASELINE_SCRIPT);
	}
}
