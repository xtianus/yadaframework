package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * 
 *
 */
public class YadaSocialException extends Exception {
	private static final long serialVersionUID = -1L;

	public YadaSocialException() {
	}

	public YadaSocialException(String message) {
		super(message);
	}

	public YadaSocialException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}
	
	public YadaSocialException(Throwable cause) {
		super(cause);
	}

	public YadaSocialException(String message, Throwable cause) {
		super(message, cause);
	}

}
