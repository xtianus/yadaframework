package net.yadaframework.security;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;

import net.yadaframework.core.YadaWebConfig;
import net.yadaframework.security.web.YadaDialectWithSecurity;

//@Configuration not needed when using WebApplicationInitializer.java
@EnableWebMvc
// Needed to use Spring Data
// @EnableSpringDataWebSupport
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = { "net.yadaframework.security.web" })
public class YadaWebSecurityConfig extends YadaWebConfig {

	@Override
	protected void addExtraDialect(SpringTemplateEngine engine) {
		engine.addDialect(new org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect());
	}
	
	@Override
	protected void addYadaDialect(SpringTemplateEngine engine) {
		engine.addDialect(new YadaDialectWithSecurity(config));
	}


}
