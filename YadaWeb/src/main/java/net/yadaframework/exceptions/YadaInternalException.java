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
