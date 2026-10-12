package net.yadaframework.core.allowlist;

import org.springframework.web.WebApplicationInitializer;

import jakarta.servlet.ServletContext;

/**
 * Records that it ran. Shares its folder with {@link SecondTestInitializer}.
 */
public class FirstTestInitializer implements WebApplicationInitializer {
	@Override
	public void onStartup(ServletContext servletContext) {
		InitializerRecorder.record(servletContext, getClass());
	}
}
