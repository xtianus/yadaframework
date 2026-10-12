package net.yadaframework.security.uploadtest.core;

import org.springframework.context.annotation.ComponentScan;

import net.yadaframework.security.YadaWebSecurityConfig;

/**
 * Servlet configuration for the upload limit integration tests.
 */
@ComponentScan(basePackages = { "net.yadaframework.security.uploadtest.web" })
public class UploadLimitTestWebConfig extends YadaWebSecurityConfig {

}
