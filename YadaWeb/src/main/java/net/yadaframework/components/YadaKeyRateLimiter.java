package net.yadaframework.components;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

/**
 * A rate limiter by key. See YadaStatelessRateLimiter for a better description.
 * <br />
 * Note: ThreadSafe
 * @see YadaSimpleRateLimiter
 */
// This is not a Component, it has to be instantiated by new
public class YadaKeyRateLimiter {
	
	// All times in milliseconds
	private long maxEvents;
	private long periodMillis;
	
	// Using IncrementableInteger to save a put in a Map
	private class IncrementableInteger {
		int value;
		IncrementableInteger(int value) {
			this.value = value;
		}
		int incrementAndGet() {
			return ++value;
		}
		int get() {
			return value;
		}
	}
	
	private Map<Integer, IncrementableInteger> eventCounterMap;
	private int mapSize = 0;
	private int maxMapSize = 0;
	private Stopwatch stopwatch;
	
	/**
	 * Create a new RateLimiter using milliseconds
	 * @param maxEvents the number of events after which the limit is applied
	 * @param periodMillis the time interval in which events are counted
	 * @param maxCacheSize Max number of different hosts that can be tracked in the period: exceeding hosts will not be monitored
	 * and will always be valid. Each element in the cache takes just 4 bytes (Integer key) + 4 bytes (IncrementableInteger). A value of 100000 should
	 * be fine (the wider the period, the higher the possible different sized probably)
	 */
	public YadaKeyRateLimiter(long maxEvents, long periodMillis, int maxCacheSize) {
		this.maxEvents = maxEvents;
		this.periodMillis = periodMillis;
		this.stopwatch = Stopwatch.createStarted();
		this.eventCounterMap = new HashMap<Integer, IncrementableInteger>();
		this.maxMapSize = maxCacheSize;
	}
	
	/**
	 * Create a new RateLimiter using the preferred unit of time
	 * @param maxEvents the number of events after which the limit is applied
	 * @param period
	 * @param periodUnit can be TimeUnit.SECONDS, TimeUnit.MINUTES etc.
	 * @param maxCacheSize Max number of different hosts that can be tracked in the period: exceeding hosts will not be monitored
	 * and will always be valid. Each element in the cache takes just 4 bytes + the hostname length. A value of 100000 should
	 * be fine (the wider the period, the higher the possible different sized probably)
	 */
	public YadaKeyRateLimiter(long maxEvents, long period, TimeUnit periodUnit, int maxCacheSize) {
		this(maxEvents, periodUnit.toMillis(period), maxCacheSize);
	}
	
	/**
	 * To be called at every event 
	 * @param key the event "name"
	 * @return true if for this key the rate is below the limit (rate valid), false otherwise (rate exceeded)
	 */
	public synchronized boolean validateRate(String key) {
		long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
		if (elapsed>periodMillis) {
			stopwatch.reset();
			eventCounterMap.clear();
			mapSize = 0;
			stopwatch.start();
			return true;
		}
		Integer keyCode = key.hashCode(); // To save memory - might create hash overlaps maybe
		IncrementableInteger elementCounter = eventCounterMap.get(keyCode);
		if (elementCounter==null && mapSize<=maxMapSize) {
			elementCounter = new IncrementableInteger(0);
			eventCounterMap.put(keyCode, elementCounter);
			mapSize++;
		}
		if (elementCounter==null) {
			return true; // mapSize>maxMapSize
		}
		int count = elementCounter.incrementAndGet();
		return (count<maxEvents);
	}
	
	/**
	 * @param key the event "name"
	 * @return the number of events that have been received in this period so far
	 */
	public synchronized long getCurrentRate(String key) {
		Integer keyCode = key.hashCode(); // To save memory - might create hash overlaps maybe
		IncrementableInteger elementCounter = eventCounterMap.get(keyCode);
		if (elementCounter!=null) {
			return elementCounter.get();
		}
		return 0;
	}
	
	/**
	 * Test and example method
	 * @param args
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		int maxEvents = 10;
		int periodSeconds = 2;
		String[] keys = {"google.com", "antrax.org"};
		// TODO sto inserendo maxCacheSize e poi devo loggare l'overflow della cache
		YadaKeyRateLimiter limiter = new YadaKeyRateLimiter(maxEvents, periodSeconds, TimeUnit.SECONDS, 100);

		long _testStartTime = System.currentTimeMillis();
		int _testLength=maxEvents*8;
		
		for (int i = 0; i < _testLength; i++) {
			String key = keys[(int) (Math.random()*2)];
			int _testSecond = (int) ((System.currentTimeMillis()-_testStartTime)/1000);
			boolean validRate = limiter.validateRate(key);
			System.out.println("@" + _testSecond + " " + key + ": " + (i+1) + " - valid = " + validRate);
			Thread.sleep((long) (periodSeconds * 1000 / (_testLength/3)));
		}
	}
	
}
