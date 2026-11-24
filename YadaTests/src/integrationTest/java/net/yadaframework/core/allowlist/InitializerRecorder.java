package net.yadaframework.core.allowlist;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletContext;

/**
 * Collects the initializers that ran, as seen from the test classloader.
 * The initializers live in the integrationTest output, which other web applications mount too:
 * they record only in applications that set the {@link #ENABLED_PARAMETER} context parameter, and do nothing elsewhere.
 */
public final class InitializerRecorder {
	public static final String ENABLED_PARAMETER = "yts.recordInitializers";
	private static final List<Class<?>> ran = new ArrayList<>();

	private InitializerRecorder() {
	}

	static synchronized void record(ServletContext servletContext, Class<?> initializerClass) {
		if ("true".equals(servletContext.getInitParameter(ENABLED_PARAMETER))) {
			ran.add(initializerClass);
		}
	}

	public static synchronized List<Class<?>> ranAndClear() {
		List<Class<?>> result = List.copyOf(ran);
		ran.clear();
		return result;
	}
}
