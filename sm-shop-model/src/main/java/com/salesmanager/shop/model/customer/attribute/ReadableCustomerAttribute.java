package com.salesmanager.shop.model.customer.attribute;

import java.io.Serial;

public class ReadableCustomerAttribute extends CustomerAttributeEntity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private ReadableCustomerOption customerOption;
	private ReadableCustomerOptionValue customerOptionValue;
	public void setCustomerOption(ReadableCustomerOption customerOption) {
		this.customerOption = customerOption;
	}
	public ReadableCustomerOption getCustomerOption() {
		return customerOption;
	}
	public void setCustomerOptionValue(ReadableCustomerOptionValue customerOptionValue) {
		this.customerOptionValue = customerOptionValue;
	}
	public ReadableCustomerOptionValue getCustomerOptionValue() {
		return customerOptionValue;
	}



}
