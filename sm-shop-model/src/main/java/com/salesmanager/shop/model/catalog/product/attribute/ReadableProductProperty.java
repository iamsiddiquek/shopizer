package com.salesmanager.shop.model.catalog.product.attribute;

import java.io.Serial;

public class ReadableProductProperty extends ProductPropertyOption {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	/** 
	 * Property use option objects
	 */
	private ReadableProductOption property = null;
	private ReadableProductPropertyValue propertyValue = null;
	public ReadableProductOption getProperty() {
		return property;
	}
	public void setProperty(ReadableProductOption property) {
		this.property = property;
	}
	public ReadableProductPropertyValue getPropertyValue() {
		return propertyValue;
	}
	public void setPropertyValue(ReadableProductPropertyValue propertyValue) {
		this.propertyValue = propertyValue;
	}



}
