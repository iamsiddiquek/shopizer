package com.salesmanager.shop.model.customer.attribute;

import java.io.Serial;
import java.io.Serializable;

public class CustomerAttributeEntity extends CustomerAttribute implements
		Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private String textValue;
	public void setTextValue(String textValue) {
		this.textValue = textValue;
	}
	public String getTextValue() {
		return textValue;
	}



}
