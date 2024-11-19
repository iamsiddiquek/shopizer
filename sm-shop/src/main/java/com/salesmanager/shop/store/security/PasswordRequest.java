package com.salesmanager.shop.store.security;

import java.io.Serial;

import javax.validation.constraints.NotEmpty;

public class PasswordRequest extends AuthenticationRequest {

	/**
	* 
	*/
	@Serial
	private static final long serialVersionUID = 1L;

  @NotEmpty(message = "{message.password.required}")
  private String current;
  
  @NotEmpty(message = "{message.password.required}")
  private String repeatPassword;

  public String getCurrent() {
    return current;
  }

  public void setCurrent(String current) {
    this.current = current;
  }

  public String getRepeatPassword() {
    return repeatPassword;
  }

  public void setRepeatPassword(String repeatPassword) {
    this.repeatPassword = repeatPassword;
  }

}
