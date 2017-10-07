package net.yadaframework.core;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

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

	// This implements servlet 3 file upload without commons fileupload but the configuration is not available here yet.
	//	@Override
	//    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
	//		MultipartConfigElement multipartConfigElement = new MultipartConfigElement(
	//			config.getUploadTempFolder(),
	//			config.getUploadMaxFileSizeBytes(),
	//			config.getUploadMaxRequestSizeBytes(),
	//			config.getUploadFileSizeThresholdBytes());
	//        registration.setMultipartConfig(multipartConfigElement);
	//    }

}
