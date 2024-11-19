package com.salesmanager.shop.model.content.page;

import java.io.Serial;

import com.salesmanager.shop.model.content.common.Content;

public class ContentPage extends Content {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private boolean linkToMenu;
	public boolean isLinkToMenu() {
		return linkToMenu;
	}
	public void setLinkToMenu(boolean linkToMenu) {
		this.linkToMenu = linkToMenu;
	}

}
