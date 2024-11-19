package com.salesmanager.shop.store.api.exception;

import java.io.Serial;

public class UnauthorizedException extends GenericRuntimeException {

  private final static String ERROR_CODE = "401";

	@Serial
	private static final long serialVersionUID = 1L;

  public UnauthorizedException() {
    super("Not authorized");
  }

  public UnauthorizedException(String errorCode, String message) {
    super(errorCode, message);
  }

  public UnauthorizedException(String message) {
    super(ERROR_CODE, message);
  }
}
