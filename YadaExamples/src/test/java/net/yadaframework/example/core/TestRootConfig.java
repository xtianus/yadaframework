package net.yadaframework.example.core;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import net.yadaframework.components.YadaMariaDBServer;
import net.yadaframework.core.YadaConfiguration;
import net.yadaframework.example.support.CapturingMailSender;
import net.yadaframework.example.support.ExampleTestDatabase;

/**
 * Root context of the example tests: exactly the root configuration classes of the real
 * {@link WebApplicationInitializer} (framework CMS and Commerce configurations, AppConfig, JpaConfig, SecurityConfig),
 * with the mail sender replaced by {@link CapturingMailSender}. The test base class sets the database properties
 * (see {@link ExampleTestDatabase}) before this context is loaded.
 */
@Configuration
@Import(TestRootConfig.ApplicationRootConfigClasses.class)
public class TestRootConfig {
	private static final int DATABASE_STARTUP_ATTEMPTS = 3;

	/**
	 * Imports the classes returned by WebApplicationInitializer.getRootConfigClasses(), so that the tests follow the application
	 */
	static class ApplicationRootConfigClasses implements ImportSelector {
		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
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

	/**
	 * @return the post processor that starts the embedded database with a new port when the chosen one was taken
	 */
	@Bean
	public static InstantiationAwareBeanPostProcessor embeddedDatabaseStartupRetry() {
		return new EmbeddedDatabaseStartupRetry();
	}

	/**
	 * Creates the "yadaMariaDBServer" bean. When the database can't start because another process took the chosen port
	 * in the meantime, it retries with a new port and an empty folder, as the YadaTests harness does.
	 */
	static class EmbeddedDatabaseStartupRetry implements InstantiationAwareBeanPostProcessor, BeanFactoryAware {
		private final Logger log = LoggerFactory.getLogger(getClass());
		private BeanFactory beanFactory;

		@Override
		public void setBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
			if (!YadaMariaDBServer.class.equals(beanClass)) {
				return null;
			}
			YadaConfiguration config = beanFactory.getBean(YadaConfiguration.class);
			for (int attempt = 1; ; attempt++) {
				int port = ExampleTestDatabase.databasePort();
				try {
					return new YadaMariaDBServer(config);
				} catch (RuntimeException e) {
					// Our own process has been stopped by YadaMariaDBServer: whoever still listens took the port
					if (attempt >= DATABASE_STARTUP_ATTEMPTS || !ExampleTestDatabase.isListening(port)) {
						throw e;
					}
					log.warn("Database port {} taken by another process: retrying with a new port", port);
					ExampleTestDatabase.renewDatabaseAllocation();
				}
			}
		}
	}
}
