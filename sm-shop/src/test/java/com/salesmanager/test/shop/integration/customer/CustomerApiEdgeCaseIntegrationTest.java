package com.salesmanager.test.shop.integration.customer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.model.customer.CustomerGender;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.customer.PersistableCustomer;
import com.salesmanager.shop.model.customer.address.Address;
import com.salesmanager.shop.store.security.AuthenticationRequest;
import com.salesmanager.shop.store.security.AuthenticationResponse;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class CustomerApiEdgeCaseIntegrationTest extends ServicesTestSupport {

	@Test
	public void registerCustomerThenLoginReturnsToken() {
		String email = "edge-login+" + System.nanoTime() + "@test.com";

		ResponseEntity<PersistableCustomer> registerResponse = testRestTemplate.postForEntity(
				"/api/v1/customer/register?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(customer(email), getHeader()),
				PersistableCustomer.class);
		assertThat(registerResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<AuthenticationResponse> loginResponse = testRestTemplate.postForEntity(
				"/api/v1/customer/login",
				new HttpEntity<>(new AuthenticationRequest(email, "clear123")),
				AuthenticationResponse.class);
		assertThat(loginResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(loginResponse.getBody());
		assertNotNull(loginResponse.getBody().getToken());
	}

	@Test
	public void customerLoginWithWrongPasswordReturnsClientError() {
		String email = "edge-bad-login+" + System.nanoTime() + "@test.com";
		ResponseEntity<PersistableCustomer> registerResponse = testRestTemplate.postForEntity(
				"/api/v1/customer/register?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(customer(email), getHeader()),
				PersistableCustomer.class);
		assertThat(registerResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<String> loginResponse = testRestTemplate.postForEntity(
				"/api/v1/customer/login",
				new HttpEntity<>(new AuthenticationRequest(email, "wrong-password")),
				String.class);
		assertThat(loginResponse.getStatusCode().is4xxClientError(), is(true));
	}

	@Test
	public void registerDuplicateCustomerEmailReturnsClientError() {
		String email = "edge-customer+" + System.nanoTime() + "@test.com";
		PersistableCustomer customer = customer(email);

		ResponseEntity<String> first = testRestTemplate.postForEntity(
				"/api/v1/customer/register",
				new HttpEntity<>(customer, getHeader()),
				String.class);
		assertThat(first.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<String> second = testRestTemplate.postForEntity(
				"/api/v1/customer/register",
				new HttpEntity<>(customer(email), getHeader()),
				String.class);
		assertThat(second.getStatusCode(), is(HttpStatus.CONFLICT));
	}

	@Test
	public void registerCustomerMalformedJsonReturnsBadRequest() {
		HttpHeaders headers = getHeader();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/customer/register?store=" + Constants.DEFAULT_STORE,
				org.springframework.http.HttpMethod.POST,
				new HttpEntity<>("{\"emailAddress\":}", headers),
				String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
	}

	private PersistableCustomer customer(String email) {
		PersistableCustomer customer = new PersistableCustomer();
		customer.setEmailAddress(email);
		customer.setPassword("clear123");
		customer.setGender(CustomerGender.M.name());
		customer.setLanguage("en");
		customer.setStoreCode(Constants.DEFAULT_STORE);
		Address billing = new Address();
		billing.setFirstName("edge");
		billing.setLastName("customer");
		billing.setCountry("US");
		customer.setBilling(billing);
		return customer;
	}
}
