package net.yadaframework.example.core;

import net.yadaframework.core.YadaJpaConfig;

import org.springframework.context.annotation.Configuration;

// Warning: do not remove @Configuration or save() operations will fail with no error message!
// TODO test if it can indeed be removed because we're using WebApplicationInitializer
@Configuration
// This can be added when using Spring Data
// @EnableJpaRepositories(basePackages = "net.yadaframework.example.persistence.repository")
// If you get a "Unknown entity" Exception, add the entity package to the config file: config/database/entityPackage
public class JpaConfig extends YadaJpaConfig {
}
