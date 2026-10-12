package net.yadaframework.tests.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaWebApplicationInitializer;

/**
 * Bootstraps the YadaTests application. The security filters are installed by the framework's
 * SecurityWebApplicationInitializer, which the test harness runs together with this class.
 */
public class WebApplicationInitializer extends YadaWebApplicationInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		if (!YadaAppConfig.getStaticConfig().isDatabaseEnabled()) {
			throw new IllegalStateException("The YadaTests application needs the embedded database");
		}
		List<Class<?>> configurationClasses = new ArrayList<>(Arrays.asList(super.getRootConfigClasses()));
		configurationClasses.add(AppConfig.class);
		configurationClasses.add(JpaConfig.class);
		configurationClasses.add(SecurityConfig.class);
		return configurationClasses.toArray(new Class<?>[0]);
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class<?>[] { WebConfig.class };
	}
}
