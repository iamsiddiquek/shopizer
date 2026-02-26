package com.salesmanager.test.shop.integration.order;

import static org.hamcrest.core.Is.is;
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
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class OrderApiEdgeCaseIntegrationTest extends ServicesTestSupport {

	@Test
	public void privateOrdersWithAdminAuthorizationReturnsOk() {
		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/private/orders?store=" + Constants.DEFAULT_STORE + "&page=0&count=5",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				String.class);

		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(response.getBody());
	}

	@Test
	public void privateOrdersWithoutAuthorizationHeaderReturnsClientError() {
		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/private/orders?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				HttpEntity.EMPTY,
				String.class);

		assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
	}

	@Test
	public void privateOrdersWithInvalidBearerTokenReturnsClientError() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", "Bearer invalid-token");

		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/private/orders?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(headers),
				String.class);

		assertThat(response.getStatusCode(), is(HttpStatus.UNAUTHORIZED));
	}
}
