package net.yadaframework.commerce.exceptions;

import org.slf4j.helpers.MessageFormatter;


/**
 * Thrown when trying to do math on non-uniform currencies or when some currency is not expected.
 *
 */
public class YadaCurrencyMismatchException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public YadaCurrencyMismatchException() {
	}

	public YadaCurrencyMismatchException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaCurrencyMismatchException(String format, Object... params) {
		super(MessageFormatter.arrayFormat(format, params).getMessage());
	}

	public YadaCurrencyMismatchException(Throwable cause, String format, Object... params) {
		super(MessageFormatter.arrayFormat(format, params).getMessage(), cause);
	}

	public YadaCurrencyMismatchException(Throwable cause) {
		super(cause);
	}

	public YadaCurrencyMismatchException(String message, Throwable cause) {
		super(message, cause);
	}

}
