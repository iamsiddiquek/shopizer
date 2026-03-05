package com.salesmanager.test.shop.integration.order;

import org.junit.Ignore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;

import com.salesmanager.shop.model.shoppingcart.ReadableShoppingCart;
import com.salesmanager.test.shop.common.ServicesTestSupport;


@Ignore
public class OrderApiIntegrationTest extends ServicesTestSupport {
	
    @Autowired
    // MIGRATION NOTE: Spring Boot 4 relocated TestRestTemplate to spring-boot-resttestclient without changing test HTTP behavior.
    private TestRestTemplate testRestTemplate;

    public void createOrder() throws Exception {
    	
    	//create cart
    	ReadableShoppingCart cart = super.sampleCart();
    	
    	//create order
    }

}
