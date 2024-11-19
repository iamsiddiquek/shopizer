package com.salesmanager.shop.model.entity;

import java.io.Serial;
import java.io.Serializable;

public abstract class ShopEntity extends Entity implements Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private String language;
	
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getLanguage() {
		return language;
	}


}
