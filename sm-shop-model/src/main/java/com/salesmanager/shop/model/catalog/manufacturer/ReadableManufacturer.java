package com.salesmanager.shop.model.catalog.manufacturer;

import java.io.Serial;
import java.io.Serializable;

public class ReadableManufacturer extends ManufacturerEntity implements
		Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private ManufacturerDescription description;
	public void setDescription(ManufacturerDescription description) {
		this.description = description;
	}
	public ManufacturerDescription getDescription() {
		return description;
	}

}
