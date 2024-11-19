package com.salesmanager.shop.store.security.common;

import java.io.Serial;

import org.springframework.security.core.AuthenticationException;

public class CustomAuthenticationException extends AuthenticationException {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	public CustomAuthenticationException(String msg) {
		super(msg);
	}

}
