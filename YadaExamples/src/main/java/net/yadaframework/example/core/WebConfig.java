package net.yadaframework.example.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.thymeleaf.spring6.SpringTemplateEngine;

import net.yadaframework.core.YadaLinkBuilder;
import net.yadaframework.security.YadaWebSecurityConfig;

@ComponentScan(basePackages = { "net.yadaframework.example.web" })
//@Configuration not needed when using WebApplicationInitializer.java
// Extend YadaWebConfig if you're not using the YadaWebSecurity project
public class WebConfig extends YadaWebSecurityConfig {

	@Override
	protected void addExtraDialect(SpringTemplateEngine engine) {
		// Add any dialects you might need
		// engine.addDialect(new LayoutDialect()); // thymeleaf-layout-dialect
	}

}

