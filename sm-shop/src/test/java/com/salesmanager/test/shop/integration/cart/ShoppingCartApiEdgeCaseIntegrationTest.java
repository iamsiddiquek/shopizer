package com.salesmanager.test.shop.integration.cart;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.shoppingcart.PersistableShoppingCartItem;
import com.salesmanager.shop.model.shoppingcart.ReadableShoppingCart;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class ShoppingCartApiEdgeCaseIntegrationTest extends ServicesTestSupport {

	@Test
	public void addToCartThenGetCartByCodeReturnsCreatedAndOk() {
		ReadableProduct product = sampleProduct("edge-cart-" + System.nanoTime());
		assertNotNull(product);

		PersistableShoppingCartItem item = new PersistableShoppingCartItem();
		item.setProduct(product.getSku());
		item.setQuantity(1);

		ResponseEntity<ReadableShoppingCart> createResponse = testRestTemplate.postForEntity(
				"/api/v1/cart?store=" + Constants.DEFAULT_STORE + "&lang=en",
				new HttpEntity<>(item, getHeader()),
				ReadableShoppingCart.class);

		assertThat(createResponse.getStatusCode(), is(HttpStatus.CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getCode());
		assertEquals(1, createResponse.getBody().getQuantity());

		ResponseEntity<ReadableShoppingCart> getResponse = testRestTemplate.exchange(
				"/api/v1/cart/" + createResponse.getBody().getCode() + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableShoppingCart.class);

		assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(getResponse.getBody());
		assertThat(getResponse.getBody().getCode(), is(createResponse.getBody().getCode()));
		assertEquals(1, getResponse.getBody().getQuantity());
	}

	@Test
	public void addToCartMalformedJsonReturnsBadRequest() {
		HttpHeaders headers = getHeader();
		headers.setContentType(MediaType.APPLICATION_JSON);

		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/cart?store=" + Constants.DEFAULT_STORE,
				HttpMethod.POST,
				new HttpEntity<>("{\"product\":}", headers),
				String.class);

		assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
	}

	@Test
	public void getUnknownCartByCodeReturnsNotFound() {
		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/cart/missing-cart-" + System.nanoTime() + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				String.class);

		assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
	}
}
