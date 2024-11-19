package com.salesmanager.shop.model.security;

import java.io.Serial;

/**
 * Object used for reading a group
 * 
 * @author carlsamson
 *
 */
public class ReadableGroup extends GroupEntity {

	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;

  private Long id = 0L;

  public void setId(Long id) {
    this.id = id;
  }

  public Long getId() {
    return id;
  }



}
