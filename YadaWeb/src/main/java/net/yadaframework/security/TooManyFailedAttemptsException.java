package net.yadaframework.security;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class TooManyFailedAttemptsException extends UsernameNotFoundException {
	private static final long serialVersionUID = 1L;
	
	public TooManyFailedAttemptsException() {
		super("");
	}

	public TooManyFailedAttemptsException(String msg) {
		super(msg);
	}

	public TooManyFailedAttemptsException(String msg, Throwable t) {
		super(msg, t);
	}

}
