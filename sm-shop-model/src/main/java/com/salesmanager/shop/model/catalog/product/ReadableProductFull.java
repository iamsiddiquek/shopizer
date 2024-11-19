package com.salesmanager.shop.model.catalog.product;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class ReadableProductFull extends ReadableProduct {

	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;
  
  List<ProductDescription> descriptions = new ArrayList<ProductDescription>();

  public List<ProductDescription> getDescriptions() {
    return descriptions;
  }

  public void setDescriptions(List<ProductDescription> descriptions) {
    this.descriptions = descriptions;
  }

}
