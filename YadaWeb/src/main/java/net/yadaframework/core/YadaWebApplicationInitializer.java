package net.yadaframework.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.builder.combined.CombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;

/**
 * Adds configuration from Yada optional projects.
 *
 */
public abstract class YadaWebApplicationInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private boolean securityProjectPresent = false;
	
	@Override
	protected Class<?>[] getRootConfigClasses() {
		List<Class<?>> configurationClasses = new ArrayList<>();
		try {
			Class.forName("net.yadaframework.security.YadaSecurityConfig");
			securityProjectPresent=true;
		} catch (ClassNotFoundException e) {
			log.info("No YadaWebSecurity project in classpath");
		}
		try {
			Class<?> theClass = Class.forName("net.yadaframework.cms.YadaCmsConfig");
			configurationClasses.add(theClass);
		} catch (ClassNotFoundException e) {
			log.info("No YadaWebCMS project in classpath");
		}
		try {
			Class<?> theClass = Class.forName("net.yadaframework.commerce.YadaCommerceConfig");
			configurationClasses.add(theClass);
		} catch (ClassNotFoundException e) {
			log.info("No YadaWebCommerce project in classpath");
		}
		return configurationClasses.toArray(new Class<?>[configurationClasses.size()]);
	}

	@Override
	protected String[] getServletMappings() {
		return new String[] { "/" };
	}

	public boolean isSecurityProjectPresent() {
		return securityProjectPresent;
	}

	// Override this to set the multipart configuration
	@Override
	protected void customizeRegistration(ServletRegistration.Dynamic registration) {
		try {
			// The configuration must be loaded outside the @Bean lifecycle because servlet registration occurs prior to this
			YadaConfiguration tempConfig = new YadaConfiguration(){};
			Parameters params = new Parameters();
			CombinedConfigurationBuilder builder = new CombinedConfigurationBuilder()
					.configure(params.fileBased().setFile(new File("configuration.xml")));
			tempConfig.setConfiguration(builder.getConfiguration());

			registration.setMultipartConfig(new MultipartConfigElement(tempConfig.getUploadsFolder().getAbsolutePath(),
					tempConfig.getMaxFileUploadSizeBytes(), tempConfig.getMaxFileUploadSizeBytes(), 0));
		} catch (ConfigurationException e) {
			log.error("Cannot load configuration, default MultipartConfig will be used", e);
			super.customizeRegistration(registration);
		}
	}
}
