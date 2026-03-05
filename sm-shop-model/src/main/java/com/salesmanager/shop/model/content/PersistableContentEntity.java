package com.salesmanager.shop.model.content;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// MIGRATION NOTE: Suppress deprecated content-model warnings because this DTO intentionally retains legacy content entity types for backward compatibility.
@SuppressWarnings("deprecation")
public class PersistableContentEntity extends ContentEntity implements Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private List<ContentDescriptionEntity> descriptions = new ArrayList<ContentDescriptionEntity>();

	public List<ContentDescriptionEntity> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<ContentDescriptionEntity> descriptions) {
		this.descriptions = descriptions;
	}

}
