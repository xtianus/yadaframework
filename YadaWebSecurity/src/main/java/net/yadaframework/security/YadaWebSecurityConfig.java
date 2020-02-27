package net.yadaframework.security;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring5.SpringTemplateEngine;

import net.yadaframework.core.YadaWebConfig;
import net.yadaframework.security.web.YadaDialectWithSecurity;

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = { "net.yadaframework.security.web" })
public class YadaWebSecurityConfig extends YadaWebConfig {

	@Override
	protected void addExtraDialect(SpringTemplateEngine engine) {
		engine.addDialect(new org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect());
	}
	
	@Override
	protected void addYadaDialect(SpringTemplateEngine engine) {
		engine.addDialect(new YadaDialectWithSecurity(config));
	}


}
