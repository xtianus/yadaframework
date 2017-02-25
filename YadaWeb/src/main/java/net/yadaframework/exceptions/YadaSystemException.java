package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * Unchecked exception thrown when the system is in error (filesystem problems, memory exceptions, etc)
 *
 */
public class YadaSystemException extends RuntimeException {
	private static final long serialVersionUID = -1L;

	public YadaSystemException() {
	}

	public YadaSystemException(String message) {
		super(message);
	}

	public YadaSystemException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}

	public YadaSystemException(Throwable cause) {
		super(cause);
	}

	public YadaSystemException(String message, Throwable cause) {
		super(message, cause);
	}

}
