package net.yadaframework.tests.core;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaConfiguration;

/**
 * Root configuration of the YadaTests application.
 * The "web" package is scanned by {@link WebConfig} only.
 */
@ComponentScan(basePackages = { "net.yadaframework.tests.components", "net.yadaframework.tests.persistence.repository" })
public class AppConfig extends YadaAppConfig {
	/** Configuration key of an optional extra Flyway location, used by tests that need a broken migration */
	public static final String EXTRA_MIGRATION_LOCATION_KEY = "config/yts/extraMigrationLocation";

	/**
	 * Creates the "config" bean reusing the static configuration, so that just one instance exists.
	 * @return the application configuration
	 */
	@Bean
	public YtsConfiguration config() {
		YadaConfiguration staticConfig = getStaticConfig();
		YtsConfiguration config = new YtsConfiguration();
		staticConfig.copyTo(config);
		CONFIG = config;
		return config;
	}

	@Override
	protected String[] getFlywayLocations() {
		List<String> locations = new ArrayList<>(List.of(super.getFlywayLocations()));
		String extraLocation = getStaticConfig().getConfiguration().getString(EXTRA_MIGRATION_LOCATION_KEY, null);
		if (extraLocation != null) {
			locations.add(extraLocation);
		}
		return locations.toArray(new String[0]);
	}
}
