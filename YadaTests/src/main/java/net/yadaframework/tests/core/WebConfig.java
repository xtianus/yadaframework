package net.yadaframework.tests.core;

import org.springframework.context.annotation.ComponentScan;

import net.yadaframework.security.YadaWebSecurityConfig;

/**
 * Servlet configuration of the YadaTests application.
 */
@ComponentScan(basePackages = { "net.yadaframework.tests.web" })
public class WebConfig extends YadaWebSecurityConfig {
}
