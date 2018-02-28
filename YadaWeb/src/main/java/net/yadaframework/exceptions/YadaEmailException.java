package net.yadaframework.exceptions;

/**
 * Runtime exception that wraps email checked exceptions.
 * Used when &lt;email throwExceptions="true">
 */
public class YadaEmailException extends RuntimeException {
	private static final long serialVersionUID = -1L;
	
	public YadaEmailException() {
	}

	public YadaEmailException(Exception e) {
		super(e);
	}

}
