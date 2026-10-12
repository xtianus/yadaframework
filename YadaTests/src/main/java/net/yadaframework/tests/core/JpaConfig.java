package net.yadaframework.tests.core;

import org.springframework.context.annotation.Configuration;

import net.yadaframework.core.YadaJpaConfig;

// @Configuration is needed for the transactional proxies of save() operations, as in YadaExamples
@Configuration
public class JpaConfig extends YadaJpaConfig {
}
