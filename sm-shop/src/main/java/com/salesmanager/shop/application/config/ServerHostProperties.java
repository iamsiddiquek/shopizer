package com.salesmanager.shop.application.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "server.host")
public class ServerHostProperties {

  @NotBlank
  private String fallback;

  public String getFallback() {
    return fallback;
  }

  public void setFallback(String fallback) {
    this.fallback = fallback;
  }
}
