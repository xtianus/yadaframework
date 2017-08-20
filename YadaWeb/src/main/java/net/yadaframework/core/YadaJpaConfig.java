package net.yadaframework.core;

import java.sql.SQLException;
import java.util.List;

import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"net.yadaframework.persistence.repository"})
@ComponentScan(basePackages = {"net.yadaframework.persistence"})
public class YadaJpaConfig {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	@Autowired YadaConfiguration config;
	
	@Bean 
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
		return new NamedParameterJdbcTemplate(dataSource);
	}
	
	@Bean 
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
	
	@Bean
	public DataSource dataSource() throws SQLException {
		JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
		log.debug("DataSource JNDI Name: {}", config.getDbJndiName());
		jndiObjectFactoryBean.setJndiName(config.getDbJndiName());
		try {
			jndiObjectFactoryBean.afterPropertiesSet();
		} catch (IllegalArgumentException | NamingException e) {
			throw new SQLException("Datasource not found", e);
		}
		return (DataSource) jndiObjectFactoryBean.getObject();
	}
	
	@Bean
	public EntityManagerFactory entityManagerFactory() throws SQLException {
		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		// vendorAdapter.setGenerateDdl(true); // Crea la tabella e le colonne quando non esistono
		vendorAdapter.setShowSql(config.getShowSql());
		vendorAdapter.setDatabasePlatform("org.hibernate.dialect.MySQL5InnoDBDialect");
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
	
	
}
