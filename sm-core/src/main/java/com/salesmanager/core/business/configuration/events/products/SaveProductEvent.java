package com.salesmanager.core.business.configuration.events.products;

import java.io.Serial;

import com.salesmanager.core.model.catalog.product.Product;

public class SaveProductEvent extends ProductEvent {
	
	public SaveProductEvent(Object source, Product product) {
		super(source, product);
	}

	@Serial
	private static final long serialVersionUID = 1L;
	
	
	

}
