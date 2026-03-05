package com.salesmanager.test.shop.common;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

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

@TestConfiguration(proxyBeanMethods = false)
public class ShopTestConfiguration {

	@Bean(name = "canadapost")
	// MIGRATION NOTE: Provides a test-only stub for the optional external canadapost starter so Boot 3 integration tests preserve the historical no-starter test setup.
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
