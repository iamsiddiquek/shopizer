package com.salesmanager.shop.model.catalog.catalog;

import java.io.Serial;

public class ReadableCatalogName extends CatalogEntity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	private String creationDate;

	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

}
