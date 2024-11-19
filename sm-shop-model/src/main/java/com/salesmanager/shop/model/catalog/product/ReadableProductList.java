package com.salesmanager.shop.model.catalog.product;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import com.salesmanager.shop.model.entity.ReadableList;

public class ReadableProductList extends ReadableList {


	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	private List<ReadableProduct> products = new ArrayList<ReadableProduct>();
	public void setProducts(List<ReadableProduct> products) {
		this.products = products;
	}
	public List<ReadableProduct> getProducts() {
		return products;
	}

}
