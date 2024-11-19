package com.salesmanager.shop.model.content;

import java.io.Serial;

/**
 * Input Object used in REST request
 * @author carlsamson
 *
 */
@Deprecated
public class ContentName extends Content {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	public ContentName() {
		super();
	}
	
	public ContentName(String name) {
		super(name);
	}
	
	public ContentName(String name, String contentType) {
		super(name);
	}

	


}
