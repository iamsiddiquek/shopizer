package com.salesmanager.core.business.configuration.events.products;

import java.io.Serial;

import org.springframework.context.ApplicationEvent;

import com.salesmanager.core.model.catalog.product.Product;

public abstract class ProductEvent extends ApplicationEvent {

	@Serial
	private static final long serialVersionUID = 1L;
	private Product product;
	
	public ProductEvent(Object source, Product product) {
		super(source);
		this.product = product;
	}


	public Product getProduct() {
		return product;
	}

}
