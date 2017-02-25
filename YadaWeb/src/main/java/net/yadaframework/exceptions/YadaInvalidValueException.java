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

	public YadaInvalidValueException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}

	public YadaInvalidValueException(Throwable cause) {
		super(cause);
	}

	public YadaInvalidValueException(String message, Throwable cause) {
		super(message, cause);
	}

}
