package com.salesmanager.shop.model.catalog.product;

import java.io.Serial;

import com.salesmanager.shop.model.catalog.NamedEntity;

public class ProductPriceDescription extends NamedEntity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	private String priceAppender;

	public String getPriceAppender() {
		return priceAppender;
	}

	public void setPriceAppender(String priceAppender) {
		this.priceAppender = priceAppender;
	}

}
