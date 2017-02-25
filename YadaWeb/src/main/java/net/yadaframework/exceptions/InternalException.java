package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * Unchecked exception thrown when something is inconsistent
 * @deprecated use YadaInternalException instead
 */
@Deprecated
public class InternalException extends RuntimeException {
	private static final long serialVersionUID = -1L;

	public InternalException() {
	}

	public InternalException(String message) {
		super(message);
	}

	public InternalException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}
	
	public InternalException(Throwable cause) {
		super(cause);
	}

	public InternalException(String message, Throwable cause) {
		super(message, cause);
	}

}
