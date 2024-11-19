package com.salesmanager.shop.model.customer;

import java.io.Serial;


public class SecuredShopPersistableCustomer extends SecuredCustomer {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	

	private String checkPassword;
	

	public String getCheckPassword() {
		return checkPassword;
	}
	public void setCheckPassword(String checkPassword) {
		this.checkPassword = checkPassword;
	}
	


}
