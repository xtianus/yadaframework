package net.yadaframework.tests.support;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.read.ListAppender;

/**
 * Records the log events of the whole JVM, including the application running in the embedded Tomcat (which shares
 * the test class loader), while it is open. Use it in try-with-resources:
 * <pre>
 * try (LogCapture logs = LogCapture.start()) {
 *     ... requests ...
 *     assertFalse(logs.text().contains(secret));
 * }
 * </pre>
 * Events are captured from DEBUG level on for the "net.yadaframework" loggers, as configured or above for the others.
 */
public final class LogCapture implements AutoCloseable {
	private static final String FRAMEWORK_LOGGER = "net.yadaframework";

	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
	private final Logger rootLogger;
	private final Logger frameworkLogger;
	private final Level previousFrameworkLevel;

	private LogCapture() {
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
		frameworkLogger = loggerContext.getLogger(FRAMEWORK_LOGGER);
		previousFrameworkLevel = frameworkLogger.getLevel();
		appender.setContext(loggerContext);
		appender.setName("yts-log-capture-" + System.identityHashCode(this));
		appender.start();
		if (!frameworkLogger.isDebugEnabled()) {
			frameworkLogger.setLevel(Level.DEBUG);
		}
		rootLogger.addAppender(appender);
	}

	/**
	 * @return a new capture, recording from now until {@link #close()}
	 */
	public static LogCapture start() {
		return new LogCapture();
	}

	/**
	 * @return the captured events, oldest first
	 */
	public List<ILoggingEvent> events() {
		synchronized (appender.list) {
			return List.copyOf(appender.list);
		}
	}

	/**
	 * @return every captured line: formatted message, argument values and stack traces, one event per line
	 */
	public String text() {
		StringBuilder result = new StringBuilder();
		for (ILoggingEvent event : events()) {
			result.append(event.getLevel()).append(' ').append(event.getLoggerName()).append(" - ").append(event.getFormattedMessage());
			IThrowableProxy throwable = event.getThrowableProxy();
			if (throwable != null) {
				result.append('\n').append(ThrowableProxyUtil.asString(throwable));
			}
			result.append('\n');
		}
		return result.toString();
	}

	/**
	 * Forgets the events captured so far
	 */
	public void clear() {
		synchronized (appender.list) {
			appender.list.clear();
		}
	}

	@Override
	public void close() {
		rootLogger.detachAppender(appender);
		appender.stop();
		frameworkLogger.setLevel(previousFrameworkLevel);
	}
}
