package com.salesmanager.shop.model.catalog.product.attribute;

import java.io.Serial;

public class ReadableProductAttributeValue extends ProductOptionValue {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String lang;
	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}


}
