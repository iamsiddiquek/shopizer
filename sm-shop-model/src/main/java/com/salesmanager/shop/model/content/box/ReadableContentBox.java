package com.salesmanager.shop.model.content.box;

import java.io.Serial;

import com.salesmanager.shop.model.content.common.Content;
import com.salesmanager.shop.model.content.common.ContentDescription;

public class ReadableContentBox extends Content {
	
	private ContentDescription description ;
	private static final String BOX = "BOX";

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	public ReadableContentBox() {
		super.setContentType(BOX);
	}

	public ContentDescription getDescription() {
		return description;
	}

	public void setDescription(ContentDescription description) {
		this.description = description;
	}

}
