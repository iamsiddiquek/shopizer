package com.salesmanager.shop.model.shoppingcart;

import java.io.Serial;

import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductOptionValue;

public class ReadableShoppingCartAttributeOptionValue extends ReadableProductOptionValue {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private String name;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}
