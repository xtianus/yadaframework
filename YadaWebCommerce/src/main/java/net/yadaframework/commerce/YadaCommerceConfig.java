package net.yadaframework.commerce;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
//import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
// Needed for Spring Data
// @EnableJpaRepositories(basePackages = "net.yadaframework.commerce.persistence.repository")
@ComponentScan(basePackages = { "net.yadaframework.commerce.persistence" })
public class YadaCommerceConfig {


}
