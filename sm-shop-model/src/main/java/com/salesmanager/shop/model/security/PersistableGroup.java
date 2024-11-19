package com.salesmanager.shop.model.security;

import java.io.Serial;

/**
 * Object used for saving a group
 * 
 * @author carlsamson
 *
 */
public class PersistableGroup extends GroupEntity {

	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;
  
  public PersistableGroup() {}
  
  public PersistableGroup(String name) {
    super.setName(name);
  }

}
