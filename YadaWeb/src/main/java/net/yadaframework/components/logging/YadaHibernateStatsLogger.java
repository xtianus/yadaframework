package net.yadaframework.components.logging;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManagerFactory;
import net.yadaframework.core.YadaConfiguration;

/**
 * Periodically emits Hibernate SessionFactory statistics while instrumentation is enabled.
 * This scheduled component runs every minute and only logs when yadaLogDbStats is true,
 * helping baseline collection without continuous overhead.
 */
@Component
public class YadaHibernateStatsLogger {
	private static final Logger log = LoggerFactory.getLogger(YadaHibernateStatsLogger.class);
	private static final long DEFAULT_DELAY_MS = 60000L;

	private final YadaConfiguration config;
	private final EntityManagerFactory entityManagerFactory;
	private boolean loggedMissingSessionFactory;

	public YadaHibernateStatsLogger(YadaConfiguration config, EntityManagerFactory entityManagerFactory) {
		this.config = config;
		this.entityManagerFactory = entityManagerFactory;
	}

	@Scheduled(initialDelay = DEFAULT_DELAY_MS, fixedDelay = DEFAULT_DELAY_MS)
	public void logStats() {
		if (!config.isYadaLogDbStatsEnabled()) {
			return;
		}
		SessionFactory sessionFactory = unwrapSessionFactory();
		if (sessionFactory == null) {
			return;
		}
		Statistics stats = sessionFactory.getStatistics();
		if (!stats.isStatisticsEnabled()) {
			return;
		}
		log.info(
			"Hibernate stats: sessionsOpened={} sessionsClosed={} queriesExecuted={} maxQueryTimeMs={} maxQuery={} entityLoads={} collectionLoads={} collectionFetches={} secondLevelCacheHits={} secondLevelCacheMisses={} transactions={}",
			stats.getSessionOpenCount(),
			stats.getSessionCloseCount(),
			stats.getQueryExecutionCount(),
			stats.getQueryExecutionMaxTime(),
			stats.getQueryExecutionMaxTimeQueryString(),
			stats.getEntityLoadCount(),
			stats.getCollectionLoadCount(),
			stats.getCollectionFetchCount(),
			stats.getSecondLevelCacheHitCount(),
			stats.getSecondLevelCacheMissCount(),
			stats.getTransactionCount());
	}

	private SessionFactory unwrapSessionFactory() {
		try {
			SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
			if (sessionFactory == null && !loggedMissingSessionFactory) {
				log.warn("Hibernate SessionFactory unavailable for statistics logging.");
				loggedMissingSessionFactory = true;
			}
			return sessionFactory;
		} catch (RuntimeException ex) {
			if (!loggedMissingSessionFactory) {
				log.warn("Hibernate SessionFactory unwrap failed for statistics logging.", ex);
				loggedMissingSessionFactory = true;
			}
			return null;
		}
	}
}
