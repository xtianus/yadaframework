package net.yadaframework.core;
import java.util.Map;

import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

@Configuration
public class YadaTestConfig extends YadaAppConfig {
	
//	public YadaTestConfig() {
//		SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
//		builder.bind("java:comp/env/jdbc/mydatasource", dataSource);
//		builder.activate();
//	}
	
	@Bean
	public YadaConfiguration config() throws ConfigurationException {
		YadaConfiguration config = new YadaConfiguration() {
			@Override
			protected void addSetupUserAttributes(Map<String, Object> user, ImmutableHierarchicalConfiguration sub) {
			}
		};
		super.makeCombinedConfiguration(config);
		return config;
	}

}
