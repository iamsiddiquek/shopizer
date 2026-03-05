package com.salesmanager.test.configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.salesmanager.core.model.common.Delivery;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.shipping.PackageDetails;
import com.salesmanager.core.model.shipping.ShippingConfiguration;
import com.salesmanager.core.model.shipping.ShippingOption;
import com.salesmanager.core.model.shipping.ShippingOrigin;
import com.salesmanager.core.model.shipping.ShippingQuote;
import com.salesmanager.core.model.system.CustomIntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationConfiguration;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.modules.integration.IntegrationException;
import com.salesmanager.core.modules.integration.shipping.model.ShippingQuoteModule;

@Configuration
@EnableAutoConfiguration
@ComponentScan({"com.salesmanager.core.business"})
@EnableJpaRepositories(basePackages = "com.salesmanager.core.business.repositories")
@EntityScan(basePackages = "com.salesmanager.core.model")
public class ConfigurationTest {
	
	// MIGRATION NOTE: Replaced the XML test utility properties bean with Java configuration so sm-core tests no longer require Spring XML imports.
	@Bean(name = "shopizer-properties")
	PropertiesFactoryBean shopizerProperties() {
		PropertiesFactoryBean bean = new PropertiesFactoryBean();
		bean.setLocation(new ClassPathResource("shopizer-properties.properties"));
		return bean;
	}

	// MIGRATION NOTE: Provides a test-only stub for the optional external canadapost starter so Boot 3 tests preserve the historical no-starter test setup.
	@Bean(name = "canadapost")
	ShippingQuoteModule canadapostTestStub() {
		return new ShippingQuoteModule() {
			@Override
			public void validateModuleConfiguration(
					IntegrationConfiguration integrationConfiguration,
					MerchantStore store) throws IntegrationException {
			}

			@Override
			public CustomIntegrationConfiguration getCustomModuleConfiguration(
					MerchantStore store) throws IntegrationException {
				return null;
			}

			@Override
			public List<ShippingOption> getShippingQuotes(
					ShippingQuote quote,
					List<PackageDetails> packages,
					BigDecimal orderTotal,
					Delivery delivery,
					ShippingOrigin origin,
					MerchantStore store,
					IntegrationConfiguration configuration,
					IntegrationModule module,
					ShippingConfiguration shippingConfiguration,
					Locale locale) throws IntegrationException {
				return null;
			}
		};
	}
	

}
