package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;


/**
 * A method parameter is invalid.
 * @deprecated use YadaInvalidException instead
 */
@Deprecated
public class InvalidValueException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidValueException() {
	}

	public InvalidValueException(String message) {
		super(message);
	}

	public InvalidValueException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}

	public InvalidValueException(Throwable cause) {
		super(cause);
	}

	public InvalidValueException(String message, Throwable cause) {
		super(message, cause);
	}

}
