package com.salesmanager.shop.model.catalog;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.salesmanager.shop.model.catalog.category.ReadableCategory;


/**
 * Object representing the results of a search query
 * @author Carl Samson
 *
 */
public class SearchProductList extends ProductList {


	@Serial
	private static final long serialVersionUID = 1L;
	private List<ReadableCategory> categoryFacets = new ArrayList<ReadableCategory>();
	public List<ReadableCategory> getCategoryFacets() {
		return categoryFacets;
	}
	public void setCategoryFacets(List<ReadableCategory> categoryFacets) {
		this.categoryFacets = categoryFacets;
	}


}
