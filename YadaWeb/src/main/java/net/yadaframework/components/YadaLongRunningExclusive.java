package net.yadaframework.components;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.yadaframework.exceptions.YadaAlreadyRunningException;

/**
 * Class that performs a long running operation that should not be invoked again before it completes.
 * It must be extended by a concrete class that implements executeInternal(). The implementation
 * may use the @Async annotation.
 */
// The subclass must be a singleton @Component
public abstract class YadaLongRunningExclusive<T> {
	private final transient Logger log = LoggerFactory.getLogger(getClass());

	private volatile boolean executing = false;
	private volatile Date lastSuccessfulRun = null;
	private volatile Exception lastError = null;
	private volatile String runningUsername = null;
	private final static Object semaphore = new Object();
	
	private T lastResult = null;

	/**
	 * Execute a long running operation that should not be invoked again before it's finished
	 * @param arguments optional arguments
	 * @throws YadaAlreadyRunningException if the operation has already been started
	 * @throws Exception any application exception
	 * @return the result (may be a Future if the implementation is @Async)
	 */
	public T execute(Object...arguments) throws Exception {
		return execute(null, arguments);
	}
	
	/**
	 * Execute a long running operation that should not be invoked again before it's finished
	 * @param runningUsername a description of the caller, so that other people can be notified of who is running the job 
	 * @param arguments optional arguments
	 * @throws YadaAlreadyRunningException if the operation has already been started
	 * @throws Exception any application exception
	 * @return the result (may be a Future if the implementation is @Async)
	 */
	public T execute(String runningUsername, Object...arguments) throws Exception {
		synchronized (semaphore) {
			if (executing) {
				throw new YadaAlreadyRunningException(runningUsername);
			}
			log.debug("Long running operation started");
			executing = true;
			this.runningUsername = runningUsername;
		}
		try {
			lastResult = executeInternal(arguments);
			lastError = null;
			lastSuccessfulRun = new Date();
			return lastResult;
		} catch (Exception e) {
			lastError = e;
			throw e;
		} finally {
			executing=false;
			log.info("Long running operation ended");
		}
	}
	
	/**
	 * This must implement the long running operation
	 * @param arguments optional arguments
	 */
	protected abstract T executeInternal(Object...arguments) throws Exception;
	
	public boolean isExecuting() {
		synchronized (semaphore) {
			return executing;
		}
	}

	/**
	 * Returns the timestamp of the last run that didn't generate an exception.
	 * @return the timestamp, or null when never run successfully.
	 */
	public Date getLastSuccessfulRun() {
		return lastSuccessfulRun;
	}

	/**
	 * Returns the exception generated in the last run, if any.
	 * @return the exception, or null if the last run was successful or never run.
	 */
	public Exception getLastError() {
		return lastError;
	}

	/**
	 * Returns the last result. Can be stale if the long running operation has yet to terminate
	 * @return the last result, may be null if the operation never terminated correctly or the result was actually null
	 */
	public T getLastResult() {
		return lastResult;
	}

	/**
	 * Set the result of the long running operation - subclasses only
	 * @param result
	 */
	protected void setLastResult(T result) {
		this.lastResult = result;
	}

	/**
	 * Get the name of the user who is running (or has last run) the task
	 * @return
	 */
	public String getRunningUsername() {
		synchronized (semaphore) {
			return runningUsername;
		}
	}
	
}
