package com.salesmanager.shop.model.content;

import java.io.Serial;

@Deprecated
public class ReadableContentEntity extends ContentEntity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private ContentDescriptionEntity description = null;
	public ContentDescriptionEntity getDescription() {
		return description;
	}
	public void setDescription(ContentDescriptionEntity description) {
		this.description = description;
	}

}
