package net.yadaframework.exceptions;

import net.yadaframework.components.YadaLongRunningExclusive;

/**
 * Thrown when a YadaLongRunningExclusive class is being called while already running
 * @see YadaLongRunningExclusive
 */
public class YadaAlreadyRunningException extends Exception {
	private static final long serialVersionUID = -7485304713501926089L;
	
	String runningUsername;

	public YadaAlreadyRunningException() {
	}

	/**
	 * Set the name of the user who is running the execution
	 * @param runningUsername
	 */
	public YadaAlreadyRunningException(String runningUsername) {
		this.runningUsername = runningUsername;
	}

	/**
	 * Get the name of the user who is running the execution
	 * @return
	 */
	public String getRunningUsername() {
		return runningUsername;
	}

}
