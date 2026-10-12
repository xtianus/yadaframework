package net.yadaframework.security.uploadtest.core;

import org.springframework.context.annotation.Bean;

import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaConfiguration;

/**
 * Root application configuration for the upload limit integration tests.
 */
public class UploadLimitTestAppConfig extends YadaAppConfig {

	/**
	 * Creates the concrete configuration bean used by the test application.
	 * @return the test configuration bean
	 */
	@Bean
	public UploadLimitTestConfiguration config() {
		YadaConfiguration staticConfig = getStaticConfig();
		UploadLimitTestConfiguration config = new UploadLimitTestConfiguration();
		staticConfig.copyTo(config);
		CONFIG = config;
		return config;
	}
}
