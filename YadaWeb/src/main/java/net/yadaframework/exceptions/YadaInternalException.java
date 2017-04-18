package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * Unchecked exception thrown when something is inconsistent
 *
 */
public class YadaInternalException extends RuntimeException {
	private static final long serialVersionUID = -1L;

	public YadaInternalException() {
	}

	public YadaInternalException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaInternalException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}
	
	public YadaInternalException(Throwable cause) {
		super(cause);
	}

	public YadaInternalException(String message, Throwable cause) {
		super(message, cause);
	}

}
