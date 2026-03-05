package com.salesmanager.core.business.configuration;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.salesmanager.core.business.modules.integration.shipping.impl.ShippingDecisionPreProcessorImpl;
import com.salesmanager.core.business.modules.integration.shipping.impl.StorePickupShippingQuote;
import com.salesmanager.core.business.modules.order.total.PromoCodeCalculatorModule;
import com.salesmanager.core.modules.order.total.OrderTotalPostProcessorModule;
import com.salesmanager.core.modules.integration.shipping.model.ShippingQuotePrePostProcessModule;

/**
 * Pre and post processors triggered during certain actions such as
 * order processing and shopping cart processing
 * 
 * 2 types of processors
 * - functions processors
 * 		Triggered during defined simple events - ex add to cart checkout
 * 
 * - calculation processors
 * 		Triggered during shopping cart and order total calculation
 * 
 * For events see configuratio/events
 * 
 * - Payment events (payment, refund)
 * 
 * - Change Order status
 * 
 * @author carlsamson
 *
 */
@Configuration
public class ProcessorsConfiguration {

	@Inject
	private PromoCodeCalculatorModule promoCodeCalculatorModule;

	@Inject
	private ShippingQuotePrePostProcessModule shippingDistancePreProcessor;

	@Inject
	private StorePickupShippingQuote storePickUp;


	/**
	 * Calculate processors
	 * @return
	 */
	@Bean
	public List<OrderTotalPostProcessorModule> orderTotalsPostProcessors() {
		
		List<OrderTotalPostProcessorModule> processors = new ArrayList<OrderTotalPostProcessorModule>();
		///processors.add(new com.salesmanager.core.business.modules.order.total.ManufacturerShippingCodeOrderTotalModuleImpl());
		processors.add(promoCodeCalculatorModule);
		return processors;
		
	}

	@Bean(name = "shippingMethodDecisionProcess")
	public ShippingDecisionPreProcessorImpl shippingMethodDecisionProcess() {
		// MIGRATION NOTE: Replaced the legacy Spring XML processor bean with Java configuration while preserving the processor type and bean id.
		return new ShippingDecisionPreProcessorImpl();
	}

	@Bean(name = "shippingModulePreProcessors")
	public List<ShippingQuotePrePostProcessModule> shippingModulePreProcessors(
			ShippingDecisionPreProcessorImpl shippingMethodDecisionProcess) {
		// MIGRATION NOTE: Replaced the legacy util:list with a Java List bean while preserving the pre-processor order.
		List<ShippingQuotePrePostProcessModule> processors = new ArrayList<ShippingQuotePrePostProcessModule>();
		processors.add(shippingDistancePreProcessor);
		processors.add(shippingMethodDecisionProcess);
		return processors;
	}

	@Bean(name = "shippingModulePostProcessors")
	public List<ShippingQuotePrePostProcessModule> shippingModulePostProcessors() {
		// MIGRATION NOTE: Replaced the legacy util:list with a Java List bean while preserving the post-processor order.
		List<ShippingQuotePrePostProcessModule> processors = new ArrayList<ShippingQuotePrePostProcessModule>();
		processors.add(storePickUp);
		return processors;
	}

}
