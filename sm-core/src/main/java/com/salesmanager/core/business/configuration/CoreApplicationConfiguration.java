package com.salesmanager.core.business.configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ComponentScan({"com.salesmanager.core.business"})
@EnableAutoConfiguration
@EnableConfigurationProperties(ApplicationSearchConfiguration.class)
@EnableJpaRepositories(basePackages = "com.salesmanager.core.business.repositories")
@EntityScan(basePackages = "com.salesmanager.core.model")
@EnableTransactionManagement
public class CoreApplicationConfiguration {

  // MIGRATION NOTE: Phase 3 removes the root Spring XML import and relies on Java configuration classes in this package instead.

}
