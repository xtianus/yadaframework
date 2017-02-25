package net.yadaframework.security;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class InternalAuthenticationException extends UsernameNotFoundException {
	private static final long serialVersionUID = 1L;

	public InternalAuthenticationException(String msg) {
		super(msg);
	}

	public InternalAuthenticationException(String msg, Throwable t) {
		super(msg, t);
	}

}
