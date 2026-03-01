package com.salesmanager.shop.application.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  @NotBlank
  @Size(min = 32)
  private String secret;

  @Positive
  private long expiration;

  @PositiveOrZero
  private int gracePeriodSeconds;

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getExpiration() {
    return expiration;
  }

  public void setExpiration(long expiration) {
    this.expiration = expiration;
  }

  public int getGracePeriodSeconds() {
    return gracePeriodSeconds;
  }

  public void setGracePeriodSeconds(int gracePeriodSeconds) {
    this.gracePeriodSeconds = gracePeriodSeconds;
  }
}
