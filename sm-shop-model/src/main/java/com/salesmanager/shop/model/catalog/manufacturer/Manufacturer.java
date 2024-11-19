package com.salesmanager.shop.model.catalog.manufacturer;

import java.io.Serial;
import java.io.Serializable;

import com.salesmanager.shop.model.entity.Entity;


public class Manufacturer extends Entity implements Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private String code;
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}

}
