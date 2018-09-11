package net.yadaframework.exceptions;
import org.slf4j.helpers.MessageFormatter;

/**
 * Convenience exception to throw when a YadaJob implementation fails execution, if the application doesn't implement a more specific exception.
 * Any other Exception would do in its place.
 */
public class YadaJobFailedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public YadaJobFailedException() {
	}

	public YadaJobFailedException(String message) {
		super(message);
	}

	/**
	 * Build the message using slf4j log format syntax
	 * @param format a string with {} placeholders for parameters
	 * @param params parameters to replace at the {} position
	 */
	public YadaJobFailedException(String format, Object... params) {
		super(MessageFormatter.arrayFormat(format, params).getMessage());
	}

	public YadaJobFailedException(Throwable cause, String format, Object... params) {
		super(MessageFormatter.arrayFormat(format, params).getMessage(), cause);
	}
	
	public YadaJobFailedException(Throwable cause) {
		super(cause);
	}

	public YadaJobFailedException(String message, Throwable cause) {
		super(message, cause);
	}
}
