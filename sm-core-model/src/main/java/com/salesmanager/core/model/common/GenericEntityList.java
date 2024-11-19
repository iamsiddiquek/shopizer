package com.salesmanager.core.model.common;

import java.io.Serial;
import java.util.List;

public class GenericEntityList<T>  extends EntityList {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	List<T> list;

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

}
