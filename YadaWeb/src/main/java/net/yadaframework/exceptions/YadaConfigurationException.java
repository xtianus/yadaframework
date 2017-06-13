package net.yadaframework.exceptions;

import org.slf4j.helpers.MessageFormatter;

/**
 * Unchecked exception thrown when some needed configuration element is missing
 *
 */
public class YadaConfigurationException extends RuntimeException {
	private static final long serialVersionUID = -1L;

	public YadaConfigurationException() {
	}

	public YadaConfigurationException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaConfigurationException(String format, Object... params) {
		super(MessageFormatter.format(format, params).getMessage());
	}
	
	public YadaConfigurationException(Throwable cause) {
		super(cause);
	}

	public YadaConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}

}
