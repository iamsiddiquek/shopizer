package com.salesmanager.shop.model.tax;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

public class ReadableTaxRateFull extends TaxRateEntity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	List<ReadableTaxRateDescription> descriptions = new ArrayList<ReadableTaxRateDescription>();
	public List<ReadableTaxRateDescription> getDescriptions() {
		return descriptions;
	}
	public void setDescriptions(List<ReadableTaxRateDescription> descriptions) {
		this.descriptions = descriptions;
	}
	


}
