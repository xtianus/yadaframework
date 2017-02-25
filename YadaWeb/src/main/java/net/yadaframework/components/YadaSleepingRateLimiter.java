package net.yadaframework.components;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.core.YadaConfiguration;

/**
 * A rate limiter that sleeps to distribute calls in a time interval.
 * The sleeping time adapts itself (shortens) depending on the time spent outside of the limiter.
 *
 */
// This is not a Component, it has to be instantiated by new
public class YadaSleepingRateLimiter {
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	// All times in milliseconds
	private long elementsLeft;
	private long endTime = -1;
	private long maxSleep = -1;
	
	/**
	 * Create a new RateLimiter using milliseconds
	 * @param totElements
	 * @param periodMillis
	 */
	public YadaSleepingRateLimiter(long totElements, long periodMillis) {
		this.elementsLeft = totElements+1; // +1 is needed to have the same sleep time after each call, also after the last one
		this.endTime = System.currentTimeMillis() + periodMillis;
	}
	
	/**
	 * Create a new RateLimiter using the preferred unit of time
	 * @param totElements
	 * @param period
	 * @param periodUnit can be YadaSleepingRateLimiter.SECONDS, YadaSleepingRateLimiter.MINUTES etc.
	 */
	public YadaSleepingRateLimiter(long totElements, long period, TimeUnit periodUnit) {
		this(totElements, periodUnit.toMillis(period));
	}

	/**
	 * Create a new RateLimiter reading values from the given configuration path, which must have attributes @hours and @maxmillis,
	 * e.g. &lt;rateLimiter hours="6" maxmillis="30000" />
	 * @param totElements
	 * @param configRateLimiterPath e.g. "config/notify/rateLimiter"
	 * @param config
	 */
	public YadaSleepingRateLimiter(long totElements, String configRateLimiterPath, YadaConfiguration config) {
		int hours = config.getInt(configRateLimiterPath + "/@hours", -999);
		this.maxSleep = config.getLong(configRateLimiterPath + "/@maxmillis", -999);
		if (hours==-999) {
			hours=8;
			log.warn("Configuration path {}/@hours not defined - using {}", configRateLimiterPath, hours);
		}
		if (this.maxSleep==-999) {
			this.maxSleep=-1;
			log.warn("Configuration path {}/@maxmillis not defined - using {}", configRateLimiterPath, this.maxSleep);
		}
		this.elementsLeft = totElements+1; // +1 is needed to have the same sleep time after each call, also after the last one
		this.endTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(hours);
		log.debug("Setting rate limiter {} to {} hours, {} maxmillis", configRateLimiterPath, hours, this.maxSleep);
	}
	
	/**
	 * The maximum amount of time that the limiter sleeps on a single invocation
	 * @param maxSleepMillis
	 */
	public void setMaxSleepMilliseconds(long maxSleepMillis) {
		this.maxSleep = maxSleepMillis;
	}
	
	public void sleepWhenNeeded() {
		if (elementsLeft>0) {
			long timeToSleep = (endTime - System.currentTimeMillis()) / elementsLeft;
			if (maxSleep>=0 && timeToSleep>maxSleep) {
				timeToSleep = maxSleep;
				log.debug("Sleeping time limited to {}", timeToSleep);
			}
			if (timeToSleep>0) {
				try {
					log.debug("Sleeping {} for element -{} ...", timeToSleep, elementsLeft);
					Thread.sleep(timeToSleep);
				} catch (InterruptedException e) {
					log.debug("Interrupted while sleeping", e);
				}
			}
			elementsLeft--;
		}
	}
	
	/**
	 * Test and example method
	 * @param args
	 */
	public static void main(String[] args) {
		int tot = 10;
		YadaSleepingRateLimiter limiter = new YadaSleepingRateLimiter(tot, 2, TimeUnit.MINUTES);
		long timeStart = System.currentTimeMillis();
		for (int i = 0; i < tot; i++) {
			System.out.println("Done " + i);
			limiter.sleepWhenNeeded();
		}
		System.out.println("Time taken: " + (System.currentTimeMillis()-timeStart)/1000 + " seconds");
	}
	
}
