package net.yadaframework.core;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.Executor;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.configuration2.builder.combined.ReloadingCombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import net.yadaframework.components.YadaUtil;

@Configuration
@ComponentScan(basePackages = { "net.yadaframework.components" })
@EnableScheduling
@EnableAsync
public class YadaAppConfig {
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired private YadaConfiguration config;
	@Autowired DataSource dataSource;
	
	@PostConstruct
	public void init() {
		// Automatic database schema migration (aka versioning).
		// See https://flywaydb.org
		if (config.useDatabaseMigrationAtStartup()) {
			log.info("Running FlyWay DB migration");
			Flyway flyway = new Flyway();
			flyway.setLocations("classpath:database"); // Where sql scripts are stored
			flyway.setDataSource(dataSource);
			flyway.migrate();
		}
	}
	
	@Bean
	public TaskScheduler taskScheduler() {
		TaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		return taskScheduler;
	}
	
	/**
	 * Creates a ThreadPoolTaskExecutor with default values.
	 * If you need to configure it, override this method in your appConfig bean.
	 */
    @Bean
    public Executor taskExecutor() {
    	return new ThreadPoolTaskExecutor();
    }
	
	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		// TODO to add more message files, use messageSource.setBasenames(String[])
		messageSource.setBasename("WEB-INF/messages/messages"); // e.g. WEB-INF/messages/messages.properties, WEB-INF/messages/messages_en.properties
		// if true, the key of the message will be displayed if the key is not
		// found, instead of throwing a NoSuchMessageException
		messageSource.setUseCodeAsDefaultMessage(true);
		messageSource.setDefaultEncoding("UTF-8");
		// # -1 : never reload, 0 always reload
		messageSource.setCacheSeconds(config.isProductionEnvironment()?600:0);
		YadaUtil.messageSource = messageSource; // Needs to be done for use outside of Beans
		return messageSource;
	}

	@Bean
	public JavaMailSender javaMailSender() {
		JavaMailSenderImpl mailSender = new org.springframework.mail.javamail.JavaMailSenderImpl();
		mailSender.setHost(config.getEmailHost());
		mailSender.setPort(config.getEmailPort());
		String emailUsername = config.getEmailUsername();
		if (emailUsername!=null) {
			mailSender.setUsername(emailUsername);
		}
		String emailPassword = config.getEmailPassword();
		if (emailPassword!=null) {
			mailSender.setPassword(emailPassword);
		}
		String emailProtocol = config.getEmailProtocol();
		if (emailProtocol!=null) {
			mailSender.setProtocol(emailProtocol);
		}
		Properties emailProperties = config.getEmailProperties();
		if (emailProperties!=null) {
			mailSender.setJavaMailProperties(emailProperties);
		}
		return mailSender;
	}
	
	/**
	 * Ritorna una istanza di YadaConfiguration. Il progetto deve dichiarare la sua sottoclasse specifica in configuration.xml e fare un override
	 * di questo metodo per ritornare il tipo castato alla sottoclasse, oltre ad aggiungere @Bean
	 * @return
	 * @throws ConfigurationException 
	 */
	protected void makeCombinedConfiguration(YadaConfiguration yadaConfiguration) throws ConfigurationException {
		Parameters params = new Parameters();
		ReloadingCombinedConfigurationBuilder builder = new ReloadingCombinedConfigurationBuilder()
			.configure(
				params.fileBased()
					.setFile(new File("configuration.xml"))
				);
		yadaConfiguration.setBuilder(builder);
//		yadaConfiguration.setConfiguration(ConfigurationUtils.unmodifiableConfiguration(builder.getConfiguration()));
		
//		builder.addEventListener(ConfigurationBuilderEvent.CONFIGURATION_REQUEST, new EventListener<Event>() {
//			@Override
//			public void onEvent(Event event) {
//				builder.getReloadingController().checkForReloading(null);
////				try {
////					yadaConfiguration.setConfiguration(ConfigurationUtils.unmodifiableConfiguration(builder.getConfiguration()));
////				} catch (ConfigurationException e) {
////					log.error("Can't reload configuration (ignored)", e);
////				}
//			}
//	    });
	}
}
