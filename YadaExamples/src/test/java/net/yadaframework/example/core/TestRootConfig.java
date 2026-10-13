package net.yadaframework.example.core;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import net.yadaframework.example.support.CapturingMailSender;
import net.yadaframework.example.support.ExampleTestDatabase;

/**
 * Root context of the example tests: exactly the root configuration classes of the real
 * {@link WebApplicationInitializer} (framework CMS and Commerce configurations, AppConfig, JpaConfig, SecurityConfig),
 * with the mail sender replaced by {@link CapturingMailSender}.
 */
@Configuration
@Import(TestRootConfig.ApplicationRootConfigClasses.class)
public class TestRootConfig {

	/**
	 * Imports the classes returned by WebApplicationInitializer.getRootConfigClasses(), so that the tests follow the application
	 */
	static class ApplicationRootConfigClasses implements ImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			// The application initializer reads the configuration, which needs the database folder and port
			ExampleTestDatabase.prepare();
			return Arrays.stream(new WebApplicationInitializer().getRootConfigClasses()).map(Class::getName).toArray(String[]::new);
		}
	}

	/**
	 * Overrides the SMTP sender of YadaAppConfig: emails are kept in memory and never sent
	 * @return the capturing sender
	 */
	@Bean
	public CapturingMailSender javaMailSender() {
		return new CapturingMailSender();
	}
}
