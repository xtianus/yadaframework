package net.yadaframework.cms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
// import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
// Needed for Spring Data
// @EnableJpaRepositories(basePackages = "net.yadaframework.cms.persistence.repository")
@ComponentScan(basePackages = { "net.yadaframework.cms.persistence", "net.yadaframework.cms.components" })
public class YadaCmsConfig {


}
