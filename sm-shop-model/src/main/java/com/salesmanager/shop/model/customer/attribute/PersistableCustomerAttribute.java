package com.salesmanager.shop.model.customer.attribute;

import java.io.Serial;

public class PersistableCustomerAttribute extends CustomerAttributeEntity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private CustomerOption customerOption;
	private CustomerOptionValue customerOptionValue;
	public void setCustomerOptionValue(CustomerOptionValue customerOptionValue) {
		this.customerOptionValue = customerOptionValue;
	}
	public CustomerOptionValue getCustomerOptionValue() {
		return customerOptionValue;
	}
	public void setCustomerOption(CustomerOption customerOption) {
		this.customerOption = customerOption;
	}
	public CustomerOption getCustomerOption() {
		return customerOption;
	}


}
