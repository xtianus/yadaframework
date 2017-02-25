package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * Unchecked exception thrown when the system is in error (filesystem problems, memory exceptions, etc)
 * @deprecated use YadaSystemException instead
 */
@Deprecated
public class SystemException extends RuntimeException {
	private static final long serialVersionUID = -1L;

	public SystemException() {
	}

	public SystemException(String message) {
		super(message);
	}

	public SystemException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}

	public SystemException(Throwable cause) {
		super(cause);
	}

	public SystemException(String message, Throwable cause) {
		super(message, cause);
	}

}
