package net.yadaframework.security.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * The current session user can't handle the object passed as parameter.
 * For example if a Controller receives an object id and this object doesn't belong to the user.
 * It could result from a bug or from tampering of the request parameters on the client.
 */
public class YadaInvalidUserException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public YadaInvalidUserException() {
	}

	public YadaInvalidUserException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaInvalidUserException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}

	public YadaInvalidUserException(Throwable cause, String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage(), cause);
	}
	
	public YadaInvalidUserException(Throwable cause) {
		super(cause);
	}

	public YadaInvalidUserException(String message, Throwable cause) {
		super(message, cause);
	}

}
