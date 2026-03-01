package com.salesmanager.shop.application.config;

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class ShopizerPropertiesConfig {

  private final Environment environment;

  public ShopizerPropertiesConfig(Environment environment) {
    this.environment = environment;
  }

  @Bean(name = "shopizer-properties")
  public Properties shopizerProperties() throws IOException {
    PropertiesFactoryBean bean = new PropertiesFactoryBean();
    bean.setLocation(new ClassPathResource("shopizer-properties.properties"));
    bean.afterPropertiesSet();

    Properties source = bean.getObject();
    Properties resolved = new Properties();
    if (source == null) {
      return resolved;
    }

    source.forEach(
        (key, value) -> resolved.put(key, environment.resolvePlaceholders(String.valueOf(value))));
    return resolved;
  }
}
