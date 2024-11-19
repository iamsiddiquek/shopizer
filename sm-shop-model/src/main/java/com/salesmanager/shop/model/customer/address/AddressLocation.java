package com.salesmanager.shop.model.customer.address;

import java.io.Serial;
import java.io.Serializable;

public class AddressLocation implements Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	private String postalCode;
	private String countryCode;
	
	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

}
