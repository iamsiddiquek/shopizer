package com.salesmanager.shop.model.references;

import java.io.Serial;

public class ReadableZone extends ZoneEntity {

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
