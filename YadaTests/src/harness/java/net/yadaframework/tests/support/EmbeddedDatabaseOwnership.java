package net.yadaframework.tests.support;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Replaces the class of the scanned "yadaMariaDBServer" bean with {@link CapturingMariaDBServer}, keeping its name,
 * laziness and dependencies. Contexts without that bean are left untouched.
 * <p>
 * Registered on the embedded Tomcat context through the "globalInitializerClasses" parameter, or applied directly
 * to a context built by a test.
 */
public class EmbeddedDatabaseOwnership implements ApplicationContextInitializer<ConfigurableApplicationContext> {
	public static final String BEAN_NAME = "yadaMariaDBServer";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		// Programmatic post-processors run after the component scan has registered the bean definitions
		applicationContext.addBeanFactoryPostProcessor(new CapturingDefinitionPostProcessor());
	}

	private static class CapturingDefinitionPostProcessor implements BeanFactoryPostProcessor {
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			if (beanFactory.containsBeanDefinition(BEAN_NAME)) {
				BeanDefinition definition = beanFactory.getBeanDefinition(BEAN_NAME);
				definition.setBeanClassName(CapturingMariaDBServer.class.getName());
			}
		}
	}
}
