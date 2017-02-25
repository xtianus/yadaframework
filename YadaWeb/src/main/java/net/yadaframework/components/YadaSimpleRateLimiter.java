package net.yadaframework.components;

import java.util.concurrent.TimeUnit;

/**
 * A simple rate limiter that just counts and resets at intervals. 
 * It is "simple" because it doesn't store the event timestamps so the rate is inaccurate (not a moving window but an average in a period).
 * Usage: call validateRate() to check if rate limit is respected.
 * <br />
 * Note: ThreadSafe
 * <br/>
 * It could for example be used to limit the logging of the same line after a while, restarting later again:
 * <pre>
 	// Log someProblem at most 50 times every 2 hours
	logRateLimiter = new YadaSimpleRateLimiter(50, 2, TimeUnit.HOURS);
	... 
	if (someProblem) {
		// Take some action then report problem
		if (logRateLimiter.validateRate()) { // Log only if not logged too much
			long skipped = logRateLimiter.getAndResetHighestInvalidCounter(); // Log lines skipped before of this one
			log.error("Problem! [{} logs skipped]", skipped);
		}
	}
 * </pre>
 * <br/>
 * Note: the rate limit could be violated in some cases, like when events occur at a burst just before the end of the period and
 * just after the start, resulting in a rate that is twice the limit. But the average will be respected regardless.
 * For example:
 * <pre>
 * maxEvents = 50
 * periodMillis = 1000
 * --> rate limit = 50 / sec
 * </pre>
 * There could be 100 valid events between millisecond 500 and 1500, resulting in an excessive burst rate of 100 / sec, but in the whole
 * interval between 0 and 2000 (two periods) the average rate would still be 100 / 2000 = 50 / sec therefore not above the limit
 */
// This is not a Component, it has to be instantiated by new
public class YadaSimpleRateLimiter {
	
	// All times in milliseconds
	private long startTime;
	private long maxEvents;
	private long periodMillis;
	
	private long eventCounter = 0;
	private long highestEventCounter = 0;
	
	/**
	 * Create a new RateLimiter using milliseconds
	 * @param maxEvents the number of events after which the limit is applied
	 * @param periodMillis the time interval in which events are counted
	 */
	public YadaSimpleRateLimiter(long maxEvents, long periodMillis) {
		this.maxEvents = maxEvents;
		this.periodMillis = periodMillis;
		this.startTime = System.currentTimeMillis();
	}
	
	/**
	 * Create a new RateLimiter using the preferred unit of time
	 * @param maxEvents the number of events after which the limit is applied
	 * @param period
	 * @param periodUnit can be TimeUnit.SECONDS, TimeUnit.MINUTES etc.
	 */
	public YadaSimpleRateLimiter(long maxEvents, long period, TimeUnit periodUnit) {
		this(maxEvents, periodUnit.toMillis(period));
	}
	
	/**
	 * To be called at every event 
	 * @return true if the rate is below the limit (rate valid), false otherwise (rate exceeded)
	 */
	public synchronized boolean validateRate() {
		long now = System.currentTimeMillis();
		long elapsed = now - startTime;
		if (elapsed>periodMillis) {
			// Reset after period
			startTime = now;
			eventCounter = 1; // The current event is counted
			return true;
		}
		eventCounter++;
		if (eventCounter>highestEventCounter && eventCounter>maxEvents) {
			highestEventCounter = eventCounter;
		}
		return (eventCounter<=maxEvents);
	}
	
	/**
	 * Return the number of events that have been flagged as invalid in this period so far
	 * @return
	 */
	public synchronized long getCurrentInvalidCounter() {
		return eventCounter<maxEvents?0:eventCounter-maxEvents;
	}
	
	/**
	 * @return the highest value of the event counter since the last call of this method.
	 */
	public synchronized long getAndResetHighestInvalidCounter() {
		long result = highestEventCounter;
		highestEventCounter = 0;
		return result;
	}
	
	/**
	 * Test and example method
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		int maxEvents = 50;
		int periodSeconds = 2;
		YadaSimpleRateLimiter limiter = new YadaSimpleRateLimiter(maxEvents, periodSeconds, TimeUnit.SECONDS);

		long _testStartTime = System.currentTimeMillis();
		int _testLength=maxEvents*4;
		
		for (int i = 0; i < _testLength; i++) {
			int _testSecond = (int) ((System.currentTimeMillis()-_testStartTime)/1000);
			boolean validRate = limiter.validateRate();
			System.out.println("@" + _testSecond + ": " + (i+1) + " - valid = " + validRate);
			Thread.sleep((long) (periodSeconds * 1000 / (_testLength/3)));
		}
	}
	
}
