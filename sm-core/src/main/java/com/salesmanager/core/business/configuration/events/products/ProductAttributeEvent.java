package com.salesmanager.core.business.configuration.events.products;

import java.io.Serial;

import com.salesmanager.core.model.catalog.product.Product;

public class ProductAttributeEvent extends ProductEvent {

	@Serial
	private static final long serialVersionUID = 1L;

	public ProductAttributeEvent(Object source, Product product) {
		super(source, product);
	}

}
