package net.yadaframework.components.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import net.yadaframework.core.YadaConfiguration;

/**
 * Applies runtime log level changes for Hibernate SQL/bind/stat loggers during a capture window.
 * This component runs at startup and on configuration reload events, toggling logger levels
 * based on config/database/yadaLogDbStats to avoid verbose logging outside instrumentation windows.
 */
@Component
public class YadaDbStatsLoggingConfigurer {
	private static final Logger log = LoggerFactory.getLogger(YadaDbStatsLoggingConfigurer.class);
	private static final String LOGGER_HIBERNATE_SQL = "org.hibernate.SQL";
	private static final String LOGGER_HIBERNATE_BIND = "org.hibernate.orm.jdbc.bind";
	private static final String LOGGER_HIBERNATE_STAT = "org.hibernate.stat";
	private static final String LOGGER_HIBERNATE_BIND_LEGACY = "org.hibernate.type.descriptor.sql.BasicBinder";

	private final YadaConfiguration config;

	public YadaDbStatsLoggingConfigurer(YadaConfiguration config) {
		this.config = config;
	}

	@PostConstruct
	public void init() {
		configure();
		config.addConfigurationReloadListener(this::configure);
	}

	public void configure() {
		boolean yadaLogDbStats = config.isYadaLogDbStatsEnabled();
		log.info("DB instrumentation flag: yadaLogDbStats={}", yadaLogDbStats);
		configureHibernateLoggers(yadaLogDbStats);
	}

	private void configureHibernateLoggers(boolean enable) {
		if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
			return;
		}
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		setLevel(context, LOGGER_HIBERNATE_SQL, enable ? Level.DEBUG : Level.WARN);
		setLevel(context, LOGGER_HIBERNATE_BIND, enable ? Level.TRACE : Level.WARN);
		setLevel(context, LOGGER_HIBERNATE_STAT, enable ? Level.INFO : Level.WARN);
		setLevel(context, LOGGER_HIBERNATE_BIND_LEGACY, enable ? Level.TRACE : Level.WARN);
	}

	private void setLevel(LoggerContext context, String loggerName, Level level) {
		context.getLogger(loggerName).setLevel(level);
	}
}
