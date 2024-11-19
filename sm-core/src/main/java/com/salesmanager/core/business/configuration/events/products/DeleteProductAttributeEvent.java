package com.salesmanager.core.business.configuration.events.products;

import java.io.Serial;

import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.attribute.ProductAttribute;

public class DeleteProductAttributeEvent extends ProductEvent {


	@Serial
	private static final long serialVersionUID = 1L;
	private ProductAttribute productAttribute;

	public DeleteProductAttributeEvent(Object source, ProductAttribute productAttribute, Product product) {
		super(source, product);
		this.productAttribute=productAttribute;
	}

	public ProductAttribute getProductAttribute() {
		return productAttribute;
	}

}
