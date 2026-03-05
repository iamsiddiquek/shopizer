package com.salesmanager.shop.store.security;

import java.io.Serial;
import java.io.Serializable;
import com.salesmanager.shop.model.entity.Entity;

public class AuthenticationResponse extends Entity implements Serializable {
  public AuthenticationResponse() {}

	/**
	*
	*/
	@Serial
	private static final long serialVersionUID = 1L;
  private String token;

  public AuthenticationResponse(Long userId, String token) {
    this.token = token;
    super.setId(userId);
  }

  public String getToken() {
    return token;
  }

  // MIGRATION NOTE: Jackson on Spring Boot 3 needs a writable token property so existing login responses deserialize without changing the JSON shape.
  public void setToken(String token) {
    this.token = token;
  }

}
