package com.salesmanager.shop.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;

// MIGRATION NOTE: Spring Boot 4 relocates SecurityAutoConfiguration to org.springframework.boot.security.autoconfigure; the explicit exclusion itself is unchanged.
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
public class ShopApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopApplication.class, args);
    }

}
