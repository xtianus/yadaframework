package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * Unchecked exception thrown when an external service returns invalid or unexpected data
 *
 */
public class YadaInvalidServiceResponseException extends RuntimeException {
	private static final long serialVersionUID = -1L;

	public YadaInvalidServiceResponseException() {
	}

	public YadaInvalidServiceResponseException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaInvalidServiceResponseException(String format, Object... params) {
		super(MessageFormatter.arrayFormat(format, params).getMessage());
	}

	public YadaInvalidServiceResponseException(Throwable cause, String format, Object... params) {
		super(MessageFormatter.arrayFormat(format, params).getMessage(), cause);
	}
	
	public YadaInvalidServiceResponseException(Throwable cause) {
		super(cause);
	}

	public YadaInvalidServiceResponseException(String message, Throwable cause) {
		super(message, cause);
	}

}
