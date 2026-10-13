package net.yadaframework.example.core;

import java.util.Arrays;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Servlet context of the example tests: the servlet configuration classes of the real {@link WebApplicationInitializer}
 */
@Configuration
@Import(TestServletConfig.ApplicationServletConfigClasses.class)
public class TestServletConfig {

	static class ApplicationServletConfigClasses implements ImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			return Arrays.stream(new WebApplicationInitializer().getServletConfigClasses()).map(Class::getName).toArray(String[]::new);
		}
	}
}
