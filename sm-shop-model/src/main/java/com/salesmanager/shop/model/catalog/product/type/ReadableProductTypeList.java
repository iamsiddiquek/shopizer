package com.salesmanager.shop.model.catalog.product.type;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.salesmanager.shop.model.entity.ReadableList;

public class ReadableProductTypeList extends ReadableList {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	List<ReadableProductType> list = new ArrayList<ReadableProductType>();

	public List<ReadableProductType> getList() {
		return list;
	}

	public void setList(List<ReadableProductType> list) {
		this.list = list;
	}

}
