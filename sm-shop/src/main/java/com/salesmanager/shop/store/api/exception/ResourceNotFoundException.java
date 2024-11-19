package com.salesmanager.shop.store.api.exception;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;

public class ResourceNotFoundException extends ServiceRuntimeException {

  private final static String ERROR_CODE = "404";
	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String errorCode, String message) {
        super(StringUtils.isBlank(errorCode)? "404": errorCode, message);
    }

    public ResourceNotFoundException(String message) {
        super(ERROR_CODE, message);
    }
}
