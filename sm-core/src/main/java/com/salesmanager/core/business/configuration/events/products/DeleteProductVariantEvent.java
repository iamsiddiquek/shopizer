package com.salesmanager.core.business.configuration.events.products;

import java.io.Serial;

import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.variant.ProductVariant;

public class DeleteProductVariantEvent extends ProductEvent {

	@Serial
	private static final long serialVersionUID = 1L;
	private ProductVariant variant;

	public DeleteProductVariantEvent(Object source, ProductVariant variant, Product product) {
		super(source, product);
		this.variant = variant;
	}

	public ProductVariant getVariant() {
		return variant;
	}

}
