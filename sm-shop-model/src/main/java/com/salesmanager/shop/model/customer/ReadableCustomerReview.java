package com.salesmanager.shop.model.customer;

import java.io.Serial;

public class ReadableCustomerReview extends CustomerReviewEntity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private ReadableCustomer reviewedCustomer;
	public ReadableCustomer getReviewedCustomer() {
		return reviewedCustomer;
	}
	public void setReviewedCustomer(ReadableCustomer reviewedCustomer) {
		this.reviewedCustomer = reviewedCustomer;
	}


}
