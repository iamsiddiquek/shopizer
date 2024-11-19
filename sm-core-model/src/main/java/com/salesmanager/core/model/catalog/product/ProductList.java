package com.salesmanager.core.model.catalog.product;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.salesmanager.core.model.common.EntityList;

public class ProductList extends EntityList {


	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 7267292601646149482L;
	private List<Product> products = new ArrayList<Product>();
	public List<Product> getProducts() {
		return products;
	}
	public void setProducts(List<Product> products) {
		this.products = products;
	}


}
