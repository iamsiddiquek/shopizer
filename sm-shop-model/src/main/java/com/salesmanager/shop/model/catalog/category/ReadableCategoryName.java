package com.salesmanager.shop.model.catalog.category;

import java.io.Serial;

public class ReadableCategoryName extends CategoryEntity {

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
