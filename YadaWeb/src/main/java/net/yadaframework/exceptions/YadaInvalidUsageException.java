package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;


/**
 * Thrown when some prerequisite is missing when calling a method.
 *
 */
public class YadaInvalidUsageException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public YadaInvalidUsageException() {
	}

	public YadaInvalidUsageException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaInvalidUsageException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}

	public YadaInvalidUsageException(Throwable cause) {
		super(cause);
	}

	public YadaInvalidUsageException(String message, Throwable cause) {
		super(message, cause);
	}

}
