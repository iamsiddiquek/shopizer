

package com.salesmanager.shop.model.catalog.product.attribute.api;

import java.io.Serial;

import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionDescription;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionEntity;

public class ReadableProductOptionEntity extends ProductOptionEntity {

	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;
  private ProductOptionDescription description;
  public ProductOptionDescription getDescription() {
    return description;
  }
  public void setDescription(ProductOptionDescription description) {
    this.description = description;
  }

}
