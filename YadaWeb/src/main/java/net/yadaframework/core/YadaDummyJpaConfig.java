package net.yadaframework.core;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import jakarta.persistence.EntityManagerFactory;

@ComponentScan(basePackages = {"net.yadaframework.persistence"})
public class YadaDummyJpaConfig {
	private DataSource dataSource = new YadaDummyDatasource();
	
	@Bean
	public NamedParameterJdbcTemplate namedParameterJdbcTemplate(DataSource dataSource) {
		return new NamedParameterJdbcTemplate(dataSource);
	}
	
	@Bean 
	public JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
	
	@Bean
	public DataSource dataSource() {
		return dataSource;
	}
	
	@Bean
	public EntityManagerFactory EntityManagerFactory() {
		return new YadaDummyEntityManagerFactory();
	}

}
