package com.salesmanager.shop.model.content;

import java.io.Serial;

// MIGRATION NOTE: Mark the subclass as deprecated alongside the legacy ContentName contract to avoid new compiler warnings without changing DTO behavior.
@Deprecated
public class ContentPath extends ContentName {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	private String path;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
