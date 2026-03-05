package com.salesmanager.shop.model.content;

import java.io.Serial;


/**
 * Model object used in webservice
 * when creatin files
 * @author carlsamson
 *
 */
// MIGRATION NOTE: This legacy content DTO remains deprecated with ContentPath to preserve existing CMS serialization contracts without introducing new compiler warnings.
@Deprecated
public class ContentFile extends ContentPath {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private byte[] file;
	

	public byte[] getFile() {
		return file;
	}
	public void setFile(byte[] file) {
		this.file = file;
	}


}
