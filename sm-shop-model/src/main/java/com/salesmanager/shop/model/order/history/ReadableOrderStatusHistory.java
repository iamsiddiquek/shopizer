package com.salesmanager.shop.model.order.history;

import java.io.Serial;


public class ReadableOrderStatusHistory extends OrderStatusHistory {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	/**
	 * YYYY-mm-DD:HH mm SSS
	 */
	private String date;

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

}
