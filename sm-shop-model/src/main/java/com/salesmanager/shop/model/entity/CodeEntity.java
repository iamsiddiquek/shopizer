package com.salesmanager.shop.model.entity;

import java.io.Serial;

public class CodeEntity extends Entity {

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
