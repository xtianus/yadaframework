package net.yadaframework.core;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

@Configuration
public class YadaTestConfig extends YadaAppConfig {
	
	public void initDatasource(YadaConfiguration config) throws NamingException {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setDatabaseName(config.getString("config/database/dbName"));
		dataSource.setUser(config.getString("config/database/user"));
		dataSource.setPassword(config.getString("config/database/password"));
		dataSource.setServerName(config.getString("config/database/server"));
		SimpleNamingContextBuilder builder = new SimpleNamingContextBuilder();
		builder.bind("java:comp/env/jdbc/yadatestdb", dataSource);
		builder.activate();
	}
	
	@Bean
	public YadaConfiguration config() throws Exception {
		YadaConfiguration config = new YadaConfiguration() {
			@Override
			protected void addSetupUserAttributes(Map<String, Object> user, ImmutableHierarchicalConfiguration sub) {
			}
		};
		super.makeCombinedConfiguration(config);
		initDatasource(config);
		return config;
	}

}
