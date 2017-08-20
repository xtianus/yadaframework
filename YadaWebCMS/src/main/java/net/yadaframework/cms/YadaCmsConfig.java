package net.yadaframework.cms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "net.yadaframework.cms.persistence.repository")
@ComponentScan(basePackages = { "net.yadaframework.cms.persistence" })
public class YadaCmsConfig {


}
