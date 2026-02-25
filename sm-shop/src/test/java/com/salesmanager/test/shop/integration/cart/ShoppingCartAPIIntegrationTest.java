package com.salesmanager.test.shop.integration.cart;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.shoppingcart.PersistableShoppingCartItem;
import com.salesmanager.shop.model.shoppingcart.ReadableShoppingCart;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ShoppingCartAPIIntegrationTest extends ServicesTestSupport {
    private static CartTestBean data = new CartTestBean();


    /**
     * Add an Item & Create cart, would give HTTP 201 & 1 qty
     *
     * @throws Exception
     */
    @Test
    @Order(1)
    public void addToCart() throws Exception {

    	ReadableProduct product = sampleProduct("addToCart");
    	assertNotNull(product);
    	data.getProducts().add(product);

        PersistableShoppingCartItem cartItem = new PersistableShoppingCartItem();
        cartItem.setProduct(product.getSku());
        cartItem.setQuantity(1);

        final HttpEntity<PersistableShoppingCartItem> cartEntity = new HttpEntity<>(cartItem, getHeader());
        final ResponseEntity<ReadableShoppingCart> response = testRestTemplate.postForEntity("/api/v1/cart".formatted(), cartEntity, ReadableShoppingCart.class);

        data.setCartId(response.getBody().getCode());

        assertNotNull(response);
        assertThat(response.getStatusCode(), is(CREATED));
        assertEquals(response.getBody().getQuantity(), 1);

    }

    /**
     * Add an second Item to existing Cart, should give HTTP 201 & 2 qty
     *
     * @throws Exception
     */
    @Test
    @Order(2)
    public void addSecondToCart() throws Exception {

        ReadableProduct product = sampleProduct("add2Cart2");
        assertNotNull(product);
        data.getProducts().add(product);

        PersistableShoppingCartItem cartItem = new PersistableShoppingCartItem();
        cartItem.setProduct(product.getSku());
        cartItem.setQuantity(1);

        final HttpEntity<PersistableShoppingCartItem> cartEntity = new HttpEntity<>(cartItem, getHeader());
        final ResponseEntity<ReadableShoppingCart> response = testRestTemplate.exchange(("/api/v1/cart/" + String.valueOf(data.getCartId())).formatted(),
                HttpMethod.PUT,
                cartEntity,
                ReadableShoppingCart.class);

        assertNotNull(response);
        assertThat(response.getStatusCode(), is(CREATED));
        assertEquals(response.getBody().getQuantity(), 2);
    }

    /**
     * Add an other item to cart which then does not exist which should return HTTP 404
     *
     * @throws Exception
     */
    @Test
    @Order(3)
    public void addToWrongToCartId() throws Exception {

        ReadableProduct product = sampleProduct("add3Cart");
        assertNotNull(product);
        data.getProducts().add(product);

        PersistableShoppingCartItem cartItem = new PersistableShoppingCartItem();
        cartItem.setProduct(product.getSku());
        cartItem.setQuantity(1);

        final HttpEntity<PersistableShoppingCartItem> cartEntity = new HttpEntity<>(cartItem, getHeader());
        final ResponseEntity<ReadableShoppingCart> response = testRestTemplate.exchange(("/api/v1/cart/" + data.getCartId() + "breakIt").formatted(),
                HttpMethod.PUT,
                cartEntity,
                ReadableShoppingCart.class);

        assertNotNull(response);
        assertThat(response.getStatusCode(), is(NOT_FOUND));
        data.getProducts().remove(product);
    }


    /**
     * Update cart items with qty 2 (1) on existing items & adding new item with qty 1 which gives result 2x2+1 = 5
     *
     * @throws Exception
     */
    @Test
    @Order(4)
    public void updateMultiWCartId() throws Exception {

        PersistableShoppingCartItem cartItem1 = new PersistableShoppingCartItem();
        cartItem1.setProduct(data.getProducts().get(0).getSku());
        cartItem1.setQuantity(2);

        PersistableShoppingCartItem cartItem2 = new PersistableShoppingCartItem();
        cartItem2.setProduct(data.getProducts().get(1).getSku());
        cartItem2.setQuantity(2);


        PersistableShoppingCartItem[] productsQtyUpdates = {cartItem1, cartItem2};


        final HttpEntity<PersistableShoppingCartItem[]> cartEntity = new HttpEntity<>(productsQtyUpdates, getHeader());
        final ResponseEntity<ReadableShoppingCart> response = testRestTemplate.exchange(("/api/v1/cart/" + data.getCartId() +
						"/multi").formatted(),
                HttpMethod.POST,
                cartEntity,
                ReadableShoppingCart.class);

        assertNotNull(response);
        assertThat(response.getStatusCode(), is(CREATED));
        assertEquals(4, response.getBody().getQuantity());
    }

    /**
     * Update cart with qnt 0 on one cart item which gives 3 qty left
     *
     * @throws Exception
     */
    @Test
    @Order(5)
    public void updateMultiWZeroOnOneProd() throws Exception {

        PersistableShoppingCartItem cartItem1 = new PersistableShoppingCartItem();
        cartItem1.setProduct(data.getProducts().get(0).getSku());
        cartItem1.setQuantity(0);

        PersistableShoppingCartItem[] productsQtyUpdates = {cartItem1};


        final HttpEntity<PersistableShoppingCartItem[]> cartEntity = new HttpEntity<>(productsQtyUpdates, getHeader());
        final ResponseEntity<ReadableShoppingCart> response = testRestTemplate.exchange(("/api/v1/cart/" + data.getCartId() +
						"/multi").formatted(),
                HttpMethod.POST,
                cartEntity,
                ReadableShoppingCart.class);

        assertNotNull(response);
        assertThat(response.getStatusCode(), is(CREATED));
        assertEquals(2, response.getBody().getQuantity());
    }

    /**
     * Delete cartitem from cart, should return 204 / no content
     *
     * @throws Exception
     */
    @Test
    @Order(6)
	    public void deleteCartItem() throws Exception {

	        final ResponseEntity<ReadableShoppingCart> response =
	                testRestTemplate.exchange(("/api/v1/cart/" + data.getCartId() + "/product/" + String.valueOf(data.getProducts().get(1).getSku())).formatted(),
	                HttpMethod.DELETE,
	                null,
	                ReadableShoppingCart.class);

        assertNotNull(response);
        assertThat(response.getStatusCode(), is(NO_CONTENT));
        assertNull(response.getBody());
    }

    /**
     * Delete cartitem from cart with body set to true which gives remaining cart content, should return 200 / ok
     *
     * @throws Exception
     */
    @Test
    @Order(7)
    public void deleteCartItemWithBody() throws Exception {

    	ReadableProduct product = sampleProduct("deleteCartItemWithBody");
    	assertNotNull(product);
    	PersistableShoppingCartItem cartItem = new PersistableShoppingCartItem();
    	cartItem.setProduct(product.getSku());
    	cartItem.setQuantity(1);
    	ResponseEntity<ReadableShoppingCart> createResponse =
    			testRestTemplate.postForEntity("/api/v1/cart", new HttpEntity<>(cartItem, getHeader()), ReadableShoppingCart.class);
    	assertThat(createResponse.getStatusCode(), is(CREATED));

        final ResponseEntity<ReadableShoppingCart> response =
                testRestTemplate.exchange(("/api/v1/cart/" + createResponse.getBody().getCode() + "/product/" + String.valueOf(product.getSku()) + "?body=true").formatted(),
                        HttpMethod.DELETE,
                        null,
                        ReadableShoppingCart.class);

        assertNotNull(response);
        assertThat(response.getStatusCode(), is(OK));
    }

    @Test
    @Order(8)
    public void getCartByCodeSupportsStoreAndLangParams() throws Exception {
        ReadableProduct product = sampleProduct("getCartByCode");
        assertNotNull(product);

        PersistableShoppingCartItem cartItem = new PersistableShoppingCartItem();
        cartItem.setProduct(product.getSku());
        cartItem.setQuantity(1);

        ResponseEntity<ReadableShoppingCart> createResponse = testRestTemplate.postForEntity(
                "/api/v1/cart?store=" + Constants.DEFAULT_STORE + "&lang=en",
                new HttpEntity<>(cartItem, getHeader()),
                ReadableShoppingCart.class);
        assertThat(createResponse.getStatusCode(), is(CREATED));
        assertNotNull(createResponse.getBody());
        assertNotNull(createResponse.getBody().getCode());

        ResponseEntity<ReadableShoppingCart> getResponse = testRestTemplate.exchange(
                "/api/v1/cart/" + createResponse.getBody().getCode() + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                ReadableShoppingCart.class);
        assertThat(getResponse.getStatusCode(), is(OK));
        assertNotNull(getResponse.getBody());
        assertThat(getResponse.getBody().getCode(), is(createResponse.getBody().getCode()));
        assertEquals(1, getResponse.getBody().getQuantity());
    }

    @Test
    @Order(9)
    public void modifyCartAndReadBackWithLocalizationParams() throws Exception {
        ReadableProduct first = sampleProduct("cartReadBack1");
        ReadableProduct second = sampleProduct("cartReadBack2");
        assertNotNull(first);
        assertNotNull(second);

        PersistableShoppingCartItem firstItem = new PersistableShoppingCartItem();
        firstItem.setProduct(first.getSku());
        firstItem.setQuantity(1);

        ResponseEntity<ReadableShoppingCart> createResponse = testRestTemplate.postForEntity(
                "/api/v1/cart?store=" + Constants.DEFAULT_STORE + "&lang=en",
                new HttpEntity<>(firstItem, getHeader()),
                ReadableShoppingCart.class);
        assertThat(createResponse.getStatusCode(), is(CREATED));
        assertNotNull(createResponse.getBody());

        PersistableShoppingCartItem secondItem = new PersistableShoppingCartItem();
        secondItem.setProduct(second.getSku());
        secondItem.setQuantity(1);

        ResponseEntity<ReadableShoppingCart> modifyResponse = testRestTemplate.exchange(
                "/api/v1/cart/" + createResponse.getBody().getCode() + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.PUT,
                new HttpEntity<>(secondItem, getHeader()),
                ReadableShoppingCart.class);
        assertThat(modifyResponse.getStatusCode(), is(CREATED));
        assertNotNull(modifyResponse.getBody());
        assertEquals(2, modifyResponse.getBody().getQuantity());

        ResponseEntity<ReadableShoppingCart> getResponse = testRestTemplate.exchange(
                "/api/v1/cart/" + createResponse.getBody().getCode() + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                ReadableShoppingCart.class);
        assertThat(getResponse.getStatusCode(), is(OK));
        assertNotNull(getResponse.getBody());
        assertEquals(2, getResponse.getBody().getQuantity());
        assertNotNull(getResponse.getBody().getProducts());
    }

}
