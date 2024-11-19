package com.salesmanager.shop.model.catalog.category;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import com.salesmanager.shop.model.entity.ReadableList;

public class ReadableCategoryList extends ReadableList {

	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;
  private List<ReadableCategory> categories = new ArrayList<ReadableCategory>();
  public List<ReadableCategory> getCategories() {
    return categories;
  }
  public void setCategories(List<ReadableCategory> categories) {
    this.categories = categories;
  }

}
