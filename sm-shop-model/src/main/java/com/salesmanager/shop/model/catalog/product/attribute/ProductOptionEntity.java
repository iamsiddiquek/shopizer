package com.salesmanager.shop.model.catalog.product.attribute;

import java.io.Serial;
import java.io.Serializable;

public class ProductOptionEntity extends ProductPropertyOption implements Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private int order;
	
	private String type;
	public void setOrder(int order) {
		this.order = order;
	}
	public int getOrder() {
		return order;
	}

	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}

}
