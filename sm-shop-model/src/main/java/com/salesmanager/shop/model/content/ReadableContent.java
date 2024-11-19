package com.salesmanager.shop.model.content;

import java.io.Serial;

/**
 * A simple piece of content
 * @author carlsamson
 *
 */
@Deprecated
public class ReadableContent extends Content {
	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
  private String content;

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

}
