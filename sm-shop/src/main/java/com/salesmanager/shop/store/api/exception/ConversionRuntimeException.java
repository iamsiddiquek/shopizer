package com.salesmanager.shop.store.api.exception;

import java.io.Serial;

public class ConversionRuntimeException extends GenericRuntimeException {

	/**
	*
	*/
	@Serial
	private static final long serialVersionUID = 1L;

  public ConversionRuntimeException(String errorCode, String message) {
    super(errorCode, message);
  }

  public ConversionRuntimeException(String message) {
    super(message);
  }

  public ConversionRuntimeException(Throwable exception) {
    super(exception);
  }

  public ConversionRuntimeException(String message, Throwable exception) {
    super(message, exception);
  }

  public ConversionRuntimeException(String errorCode, String message, Throwable exception) {
    super(errorCode, message, exception);
  }
}
