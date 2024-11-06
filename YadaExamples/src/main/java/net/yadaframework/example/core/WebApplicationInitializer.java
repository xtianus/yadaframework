package net.yadaframework.example.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaDummyJpaConfig;
import net.yadaframework.core.YadaWebApplicationInitializer;
import net.yadaframework.exceptions.YadaConfigurationException;

public class WebApplicationInitializer extends YadaWebApplicationInitializer {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Configuration for the ROOT context
	 */
	@Override
	protected Class<?>[] getRootConfigClasses() {
		List<Class<?>> configurationClasses = new ArrayList<>();
		configurationClasses.addAll(Arrays.asList(super.getRootConfigClasses()));
		configurationClasses.add(AppConfig.class);
		if (YadaAppConfig.getStaticConfig().isDatabaseEnabled()) {
			configurationClasses.add(JpaConfig.class);
		} else {
			// Still need to add a dummy datasource for all DAOs that are going to be instantiated
			configurationClasses.add(YadaDummyJpaConfig.class);
			log.info("Database disabled");
		}
		try {
			Class<?> theClass = Class.forName("net.yadaframework.example.core.SecurityConfig");
			configurationClasses.add(theClass);
		} catch (ClassNotFoundException e) {
			if (super.isSecurityProjectPresent()) {
				throw new YadaConfigurationException("The YadaSecurityProject is in the classpath but no application-specific SecurityConfig has been found");
			}
			log.warn("No application security configured");
		}
		return configurationClasses.toArray(new Class<?>[configurationClasses.size()]);
	}

	/**
	 * Configuration for the SERVLET context
	 */
	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class<?>[] { WebConfig.class };
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

}
