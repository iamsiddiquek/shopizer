package com.salesmanager.shop.model.customer;

import java.io.Serial;

public class UserAlreadyExistException extends Exception {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	public UserAlreadyExistException(String message) {
		super(message,null);
	}

}
