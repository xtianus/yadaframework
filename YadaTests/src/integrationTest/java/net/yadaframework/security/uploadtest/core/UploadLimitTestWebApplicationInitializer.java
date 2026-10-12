package net.yadaframework.security.uploadtest.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.DelegatingFilterProxy;
import org.springframework.web.multipart.support.MultipartFilter;

import jakarta.servlet.Filter;
import net.yadaframework.core.YadaAppConfig;
import net.yadaframework.core.YadaDummyJpaConfig;
import net.yadaframework.core.YadaWebApplicationInitializer;
import net.yadaframework.security.AuditFilter;
import net.yadaframework.security.CheckSessionFilter;
import net.yadaframework.security.components.YadaAuthenticationSuccessFilter;

/**
 * Bootstraps the upload limit integration test web application.
 */
public class UploadLimitTestWebApplicationInitializer extends YadaWebApplicationInitializer {

	@Override
	protected Class<?>[] getRootConfigClasses() {
		List<Class<?>> configurationClasses = new ArrayList<>();
		configurationClasses.addAll(Arrays.asList(super.getRootConfigClasses()));
		configurationClasses.add(UploadLimitTestAppConfig.class);
		if (YadaAppConfig.getStaticConfig().isDatabaseEnabled()) {
			throw new IllegalStateException("The upload limit integration test must run with database disabled");
		}
		configurationClasses.add(YadaDummyJpaConfig.class);
		configurationClasses.add(UploadLimitTestSecurityConfig.class);
		return configurationClasses.toArray(new Class<?>[configurationClasses.size()]);
	}

	@Override
	protected Class<?>[] getServletConfigClasses() {
		return new Class<?>[] { UploadLimitTestWebConfig.class };
	}

	/**
	 * Installs the security-related servlet filters inside the single test initializer.
	 * @return the servlet filters required by the upload limit integration test
	 */
	@Override
	protected Filter[] getServletFilters() {
		CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
		characterEncodingFilter.setEncoding("UTF-8");
		characterEncodingFilter.setForceEncoding(true);
		return new Filter[] {
			new CheckSessionFilter(),
			characterEncodingFilter,
			new AuditFilter(),
			new MultipartFilter(),
			new DelegatingFilterProxy("yadaLocalePathVariableFilter"),
			new YadaAuthenticationSuccessFilter(),
			new DelegatingFilterProxy("springSecurityFilterChain")
		};
	}
}
