

package com.salesmanager.shop.model.catalog.product.attribute.api;

import java.io.Serial;

import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription;

public class ReadableProductOptionValue extends ProductOptionValueEntity {

  /**
   * 
   */
  private String price;
	@Serial
	private static final long serialVersionUID = 1L;
  private ProductOptionValueDescription description;
  public ProductOptionValueDescription getDescription() {
    return description;
  }
  public void setDescription(ProductOptionValueDescription description) {
    this.description = description;
  }
public String getPrice() {
	return price;
}
public void setPrice(String price) {
	this.price = price;
}

}
