package net.yadaframework.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring4.SpringTemplateEngine;

import net.yadaframework.core.YadaWebConfig;
import net.yadaframework.web.YadaDialectWithSecurity;

@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
@EnableScheduling
@EnableAsync
public class YadaWebSecurityConfig extends YadaWebConfig {

	@Override
	protected void addExtraDialect(SpringTemplateEngine engine) {
		engine.addDialect(new org.thymeleaf.extras.springsecurity4.dialect.SpringSecurityDialect());
	}
	
	@Override
	protected void addYadaDialect(SpringTemplateEngine engine) {
		engine.addDialect(new YadaDialectWithSecurity(config));
	}


}
