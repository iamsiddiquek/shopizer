package com.salesmanager.shop.model.order.v1;

import java.io.Serial;

import com.salesmanager.shop.model.customer.PersistableCustomer;

public class PersistableAnonymousOrder extends PersistableOrder {

	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;
  
  private PersistableCustomer customer;

  public PersistableCustomer getCustomer() {
    return customer;
  }

  public void setCustomer(PersistableCustomer customer) {
    this.customer = customer;
  }

}
