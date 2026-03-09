package net.yadaframework.core;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.configuration2.ImmutableHierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
// import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.hibernate5.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.vibur.dbcp.ViburDBCPDataSource;

import net.yadaframework.components.YadaMariaDBServer;
import net.yadaframework.components.logging.YadaTraceStatementInspector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

//@Configuration not needed when using WebApplicationInitializer.java
@EnableTransactionManagement
// Needed for Spring Data
// @EnableJpaRepositories(basePackages = {"net.yadaframework.persistence.repository"})
@ComponentScan(basePackages = {"net.yadaframework.persistence"})
public class YadaJpaConfig {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired YadaConfiguration config;
	@Autowired ApplicationContext applicationContext;
	private DataSource dataSource = null;

	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
		return new NamedParameterJdbcTemplate(dataSource);
	}
	
	@Bean 
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
	
	/**
	 * Returns the datasource. It can either be configured in the application configuration (uses vibur-dbcp pool) or on JNDI
	 * via META-INF/context.xml (uses Tomcat-jndi pool)
	 * @return
	 * @throws SQLException
	 */
	@Bean
	public DataSource dataSource() throws SQLException {
		// Configuration DataSource
		DataSource result = getProgrammaticDatasource();
		if (result!=null) {
			log.info("DataSource from configuration file (not JNDI)");
			return result;
		}
		// JNDI DataSource
		JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
		log.info("DataSource JNDI Name: {}", config.getDbJndiName());
		jndiObjectFactoryBean.setJndiName(config.getDbJndiName());
		try {
			jndiObjectFactoryBean.afterPropertiesSet();
		} catch (IllegalArgumentException | NamingException e) {
			throw new SQLException("Datasource not found", e);
		}
		return (DataSource) jndiObjectFactoryBean.getObject();
	}
	
	/**
	 * Builds the entity manager factory used by Spring at startup.
	 * Enables SQL comments and Hibernate statistics only when yadaLogDbStats capture is active.
	 */
	@Bean
	public EntityManagerFactory entityManagerFactory() throws SQLException {
		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		// vendorAdapter.setGenerateDdl(true); // Crea la tabella e le colonne quando non esistono
		vendorAdapter.setShowSql(config.getShowSql());
		// HHH90000025: MySQLDialect does not need to be specified explicitly
		// vendorAdapter.setDatabasePlatform("org.hibernate.dialect.MySQLDialect");
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setJpaVendorAdapter(vendorAdapter);
		List<String> packages = config.getDbEntityPackages();
		packages.add("net.yadaframework.persistence.entity");
		packages.add("net.yadaframework.security.persistence.entity");
		packages.add("net.yadaframework.cms.persistence.entity");
		packages.add("net.yadaframework.commerce.persistence.entity");
		log.info("Scanning packages for entities: {}", StringUtils.join(packages, ","));
		factory.setPackagesToScan(packages.toArray(new String[]{}));
		factory.setDataSource(dataSource());
		
		boolean yadaLogDbStats = config.isYadaLogDbStatsEnabled();
		Map<String, Object> jpaProperties = new HashMap<>();
		jpaProperties.put("hibernate.use_sql_comments", yadaLogDbStats);
		if (yadaLogDbStats) {
			jpaProperties.put("hibernate.session_factory.statement_inspector", YadaTraceStatementInspector.class.getName());
		}
		jpaProperties.put("hibernate.generate_statistics", yadaLogDbStats);
		factory.setJpaPropertyMap(jpaProperties);
		factory.afterPropertiesSet();
		return factory.getObject();
	}	

	@Bean
	public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
		return entityManagerFactory.createEntityManager();
	}

	@Bean
	public PlatformTransactionManager transactionManager() throws SQLException {
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory());
		return txManager;
	}

	@Bean
	public HibernateExceptionTranslator hibernateExceptionTranslator() {
		return new HibernateExceptionTranslator();
	}
	
	/**
	 * Returns a DataSource that has NOT been configured on JNDI. Given that there is a configuration file for each environment, you
	 * could have a programmatic datasource in development and a JNDI datasource in production, if needed.
	 * This method should be overridden to set more parameters than currently implemented.
	 * @return null if the DataSource is on JNDI (via context.xml), or a new Vibur DataSource otherwise
	 */
	public synchronized DataSource getProgrammaticDatasource() {
		if (dataSource!=null) {
			return dataSource; // Keep the instance in case it is called twice (it happened)
		}
		try {
			Integer port = null;
			if (config.isUseEmbeddedDatabase()) {
				try {
					// Only if the mariadb jar is in the classpath
					Class<?> theClass = Class.forName("ch.vorburger.mariadb4j.DB");
					YadaMariaDBServer yadaMariaDBServer = (YadaMariaDBServer) applicationContext.getBean("yadaMariaDBServer");
					port = yadaMariaDBServer.getPort();
				} catch (ClassNotFoundException e) {
					log.error("No MariaDB in classpath while trying to use the embedded database (ignoring)");
				}
			}
			
			ImmutableHierarchicalConfiguration datasourceConfig = config.getConfiguration().immutableConfigurationAt("config/database/datasource");
			String jdbcUrl = datasourceConfig.getString("jdbcUrl");
			if (port!=null && port>0) {
				// Forcing localhost at a specific port when using embedded db
				jdbcUrl = jdbcUrl.replaceAll("//[^:/]+(:\\d+)?/", "//localhost:" + port + "/");
			}
			
			ViburDBCPDataSource ds = new ViburDBCPDataSource();
			ds.setJdbcUrl(jdbcUrl);
			ds.setUsername(datasourceConfig.getString("username"));
			ds.setPassword(datasourceConfig.getString("password"));
			ds.setName(datasourceConfig.getString("name")); // Pool name

			ds.setPoolInitialSize(datasourceConfig.getInt("poolInitialSize"));
			ds.setPoolMaxSize(datasourceConfig.getInt("poolMaxSize"));
			ds.setPoolEnableConnectionTracking(datasourceConfig.getBoolean("poolEnableConnectionTracking"));

			ds.setLogQueryExecutionLongerThanMs(datasourceConfig.getInt("logQueryExecutionLongerThanMs"));
			ds.setLogStackTraceForLongQueryExecution(datasourceConfig.getBoolean("logStackTraceForLongQueryExecution"));
			ds.setLogLargeResultSet(datasourceConfig.getLong("logLargeResultSet"));
			ds.setLogStackTraceForLargeResultSet(datasourceConfig.getBoolean("logStackTraceForLargeResultSet"));
			ds.setIncludeQueryParameters(datasourceConfig.getBoolean("includeQueryParameters"));

			ds.setStatementCacheMaxSize(datasourceConfig.getInt("statementCacheMaxSize"));
			// ds.setDriverClassName("com.mysql.cj.jdbc.Driver"); // Not needed

			ds.start();
			this.dataSource = ds;
			return ds;
		} catch (org.apache.commons.configuration2.ex.ConfigurationRuntimeException e) {
			log.info("No datasource in application configuration - using JNDI");
		}
	    return null;
	}
	
}
