package net.yadaframework.core;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.apache.commons.configuration2.builder.combined.ReloadingCombinedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import jakarta.annotation.PostConstruct;
import net.yadaframework.components.YadaUtil;
import net.yadaframework.web.dialect.YadaDialect;

//@Configuration not needed when using WebApplicationInitializer.java
@ComponentScan(basePackages = { "net.yadaframework.components" })
@EnableScheduling
@EnableAsync
public class YadaAppConfig {
	private final static Logger log = LoggerFactory.getLogger(YadaAppConfig.class);
	
	// Static instance of the configuration to use when the ApplicationContext is not (yet) available
	protected static YadaConfiguration CONFIG = null;

	@Autowired private YadaConfiguration config;
	@Autowired protected DataSource dataSource;
	@Autowired ApplicationContext applicationContext;

	@PostConstruct
	public void init() {
		// Inject the applicationContext in YadaUtil because it may be used before it's injected by Spring
		YadaUtil.applicationContext = applicationContext;

		// Automatic database schema migration (aka versioning).
		// See https://flywaydb.org
		if (config.useDatabaseMigrationAtStartup()) {
			log.info("Running FlyWay DB migration");
			// Flyway flyway = Flyway.configure().dataSource(url, user, password).load();
			Flyway flyway = Flyway.configure()
				.dataSource(dataSource)
				.cleanDisabled(true) // Just to be safe
				.locations("classpath:database") // Where sql scripts are stored
				.outOfOrder(config.useDatabaseMigrationOutOfOrder()) // Apply new migrations with lower number added later. Needed for parallel development.
				// If the db is not empty and there is no metadata, add the metadata instead of failing, setting the version to 1
				.baselineOnMigrate(true)
				.table(config.flywayTableName())
				// Add all Spring-instantiated JavaMigration beans
			    .javaMigrations(applicationContext.getBeansOfType(JavaMigration.class).values().toArray(new JavaMigration[0]))
				.load();
			flyway.migrate();
		}
	}
	
	/**
	 * Initialize and return a static instance of YadaConfiguration for internal use.
	 * @see YadaUtil.getBean() for an application access to the "config" bean instance.
	 */
	public static synchronized YadaConfiguration getStaticConfig() {
		if (CONFIG==null) {
			try {
				CONFIG = new YadaConfiguration() {}; // Anonymous subclass is needed because YadaConfiguration is abstract
				makeCombinedConfiguration(CONFIG);
				log.debug("Statically loaded configuration.xml");
			} catch (Exception e) {
				log.debug("Failed to statically load configuration.xml", e);
			}
		}
		return CONFIG;
	}

	public ClassLoaderTemplateResolver emailTemplateResolver() {
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		// Relative paths never work, with or without trailing slash, so better to be consistent without and always use "absolute" paths [xtian]
		resolver.setPrefix(YadaConstants.EMAIL_TEMPLATES_PREFIX); // Attenzione allo slash finale!
//		resolver.setPrefix(YadaConstants.EMAIL_TEMPLATES_PREFIX + "/"); // Attenzione allo slash finale!
		Set<String> patterns = new HashSet<>();
		patterns.add("/email/*"); // Start with "email"
		resolver.setResolvablePatterns(patterns);
		resolver.setSuffix(".html");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(config.isProductionEnvironment());
		// resolver.setOrder(40); // Order not needed because resolver on different SpringTemplateEngine
		return resolver;
	}

	@Bean
	public SpringTemplateEngine emailTemplateEngine() {
		SpringTemplateEngine engine = new SpringTemplateEngine();
		engine.setEnableSpringELCompiler(true);
		engine.addTemplateResolver(emailTemplateResolver());
		// Do this in the subclass
		//		// http://www.thymeleaf.org/layouts.html
		//		engine.addDialect(new LayoutDialect()); // thymeleaf-layout-dialect
		addExtraDialect(engine); // thymeleaf-SpringSecurity-dialect
		engine.addDialect(new YadaDialect(config));
		return engine;
	}

	/**
	 * To be overridden when a new dialect has to be added, e.g. engine.addDialect(new LayoutDialect());
	 * @param engine
	 */
	protected void addExtraDialect(SpringTemplateEngine engine) {
		// Do nothing
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
		String[] baseNames = config.getMessageSourceBasenames();
		// Add the "WEB-INF/messages" prefix when missing
		for (int i=0; i<baseNames.length; i++) {
			if (!baseNames[i].startsWith("WEB-INF/messages")) {
				baseNames[i] = "WEB-INF/messages/" + baseNames[i];
			}
		}
		messageSource.setBasenames(baseNames); // e.g. WEB-INF/messages/messages.properties, WEB-INF/messages/messages_de.properties
		// if true, the key of the message will be displayed if the key is not
		// found, instead of throwing a NoSuchMessageException
		messageSource.setFallbackToSystemLocale(false);
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
	protected static void makeCombinedConfiguration(YadaConfiguration yadaConfiguration) throws ConfigurationException {
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
