package com.salesmanager.shop.model.catalog.product.attribute;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PersistableProductOption extends ProductOptionEntity implements
		Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private List<ProductOptionDescription> descriptions = new ArrayList<ProductOptionDescription>();
	public void setDescriptions(List<ProductOptionDescription> descriptions) {
		this.descriptions = descriptions;
	}
	public List<ProductOptionDescription> getDescriptions() {
		return descriptions;
	}

}
