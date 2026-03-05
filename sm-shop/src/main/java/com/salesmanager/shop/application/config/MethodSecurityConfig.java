package com.salesmanager.shop.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
// MIGRATION NOTE: Spring Security 7 replaces EnableGlobalMethodSecurity with EnableMethodSecurity; method authorization behavior is unchanged.
@EnableMethodSecurity(
  prePostEnabled = true, 
  securedEnabled = true, 
  jsr250Enabled = true)
public class MethodSecurityConfig {

}
