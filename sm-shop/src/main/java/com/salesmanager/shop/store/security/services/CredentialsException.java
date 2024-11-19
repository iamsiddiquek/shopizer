package com.salesmanager.shop.store.security.services;

import java.io.Serial;

public class CredentialsException extends Exception {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	public CredentialsException(String message) {
		super(message);
	}
	
	public CredentialsException(String message, Exception e) {
		super(message, e);
	}

}
