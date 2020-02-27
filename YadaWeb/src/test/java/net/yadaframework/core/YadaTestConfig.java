package net.yadaframework.core;
import javax.annotation.PostConstruct;
import javax.naming.NamingException;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import com.mysql.cj.jdbc.MysqlDataSource;

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
		super.dataSource = dataSource;
		builder.activate();
		// Database
		Flyway flyway = new Flyway();
		flyway.setLocations("filesystem:schema"); // Where sql test scripts are stored
		flyway.setDataSource(dataSource);
		flyway.clean();
		flyway.migrate();
	}
	
	@Override
	@PostConstruct
	public void init() {
		// Prevents the normal schema migration
	}
	
	@Bean
	public YadaConfiguration config() throws Exception {
		YadaConfiguration config = new YadaConfiguration() {};
		super.makeCombinedConfiguration(config);
		initDatasource(config);
		return config;
	}

}
