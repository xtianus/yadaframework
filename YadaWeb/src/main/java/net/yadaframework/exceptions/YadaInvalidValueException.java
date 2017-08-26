package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;


/**
 * A method parameter is invalid.
 *
 */
public class YadaInvalidValueException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public YadaInvalidValueException() {
	}

	public YadaInvalidValueException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaInvalidValueException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}

	public YadaInvalidValueException(Throwable cause, String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage(), cause);
	}
	
	public YadaInvalidValueException(Throwable cause) {
		super(cause);
	}

	public YadaInvalidValueException(String message, Throwable cause) {
		super(message, cause);
	}

}
