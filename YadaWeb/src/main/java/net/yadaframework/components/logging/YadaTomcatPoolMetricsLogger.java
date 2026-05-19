package net.yadaframework.components.logging;

import java.lang.reflect.Method;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.yadaframework.core.YadaConfiguration;

/**
 * Periodically logs Tomcat JDBC pool metrics when db stats capture is enabled.
 * The logger runs on a fixed schedule and uses the Spring DataSource bean; it only
 * emits metrics for Tomcat JDBC pools during yadaLogDbStats capture windows.
 */
@Component
public class YadaTomcatPoolMetricsLogger {
	private static final Logger log = LoggerFactory.getLogger(YadaTomcatPoolMetricsLogger.class);
	private static final long DEFAULT_DELAY_MS = 60000L;
	private static final String TOMCAT_DATASOURCE_PACKAGE = "org.apache.tomcat.jdbc.pool.";

	private final YadaConfiguration config;
	private final DataSource dataSource;
	private boolean warnedUnsupportedPool;

	public YadaTomcatPoolMetricsLogger(YadaConfiguration config, DataSource dataSource) {
		this.config = config;
		this.dataSource = dataSource;
	}

	@Scheduled(initialDelay = DEFAULT_DELAY_MS, fixedDelay = DEFAULT_DELAY_MS)
	public void logPoolMetrics() {
		if (!config.isYadaLogDbStatsEnabled()) {
			return;
		}
		TomcatPoolMetrics metrics = readTomcatMetrics(dataSource);
		if (metrics == null) {
			if (!warnedUnsupportedPool) {
				log.debug("DB pool metrics disabled: unsupported DataSource type {}",
					dataSource == null ? "null" : dataSource.getClass().getName());
				warnedUnsupportedPool = true;
			}
			return;
		}
		log.info("DB pool metrics: active={} idle={} waitCount={} maxWait={} maxTotal={}",
			metrics.active, metrics.idle, metrics.waitCount, metrics.maxWait, metrics.maxTotal);
	}

	private TomcatPoolMetrics readTomcatMetrics(DataSource dataSource) {
		if (dataSource == null || !dataSource.getClass().getName().startsWith(TOMCAT_DATASOURCE_PACKAGE)) {
			return null;
		}
		int active = tryInvokeInt(dataSource, "getActive");
		int idle = tryInvokeInt(dataSource, "getIdle");
		long waitCount = tryInvokeLong(dataSource, "getWaitCount");
		Object poolProperties = tryInvoke(dataSource, "getPoolProperties");
		int maxWait = poolProperties == null ? -1 : tryInvokeInt(poolProperties, "getMaxWait");
		int maxTotal = poolProperties == null ? -1 : tryInvokeInt(poolProperties, "getMaxActive");
		if (maxTotal < 0 && poolProperties != null) {
			maxTotal = tryInvokeInt(poolProperties, "getMaxTotal");
		}
		return new TomcatPoolMetrics(active, idle, waitCount, maxWait, maxTotal);
	}

	private Object tryInvoke(Object target, String methodName) {
		if (target == null) {
			return null;
		}
		try {
			Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		} catch (ReflectiveOperationException ex) {
			return null;
		}
	}

	private int tryInvokeInt(Object target, String methodName) {
		Object value = tryInvoke(target, methodName);
		return value instanceof Number ? ((Number) value).intValue() : -1;
	}

	private long tryInvokeLong(Object target, String methodName) {
		Object value = tryInvoke(target, methodName);
		return value instanceof Number ? ((Number) value).longValue() : -1L;
	}

	private static final class TomcatPoolMetrics {
		private final int active;
		private final int idle;
		private final long waitCount;
		private final int maxWait;
		private final int maxTotal;

		private TomcatPoolMetrics(int active, int idle, long waitCount, int maxWait, int maxTotal) {
			this.active = active;
			this.idle = idle;
			this.waitCount = waitCount;
			this.maxWait = maxWait;
			this.maxTotal = maxTotal;
		}
	}
}
