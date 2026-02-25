package com.salesmanager.test.shop.integration.customer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;

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
import com.salesmanager.core.model.customer.CustomerGender;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.customer.PersistableCustomer;
import com.salesmanager.shop.model.customer.ReadableCustomer;
import com.salesmanager.shop.model.customer.address.Address;
import com.salesmanager.shop.populator.customer.ReadableCustomerList;
import com.salesmanager.shop.store.security.AuthenticationRequest;
import com.salesmanager.shop.store.security.AuthenticationResponse;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class CustomerRegistrationIntegrationTest extends ServicesTestSupport {

    @Test
    public void registerCustomer() {
        
      
        final PersistableCustomer testCustomer = new PersistableCustomer();
        String uniqueEmail = "customer+" + System.nanoTime() + "@test.com";
        testCustomer.setEmailAddress(uniqueEmail);
        testCustomer.setPassword("clear123");
        testCustomer.setGender(CustomerGender.M.name());
        testCustomer.setLanguage("en");
        final Address billing = new Address();
        billing.setFirstName("customer1");
        billing.setLastName("ccstomer1");
        billing.setCountry("BE");
        testCustomer.setBilling(billing);
        testCustomer.setStoreCode(Constants.DEFAULT_STORE);
        final HttpEntity<PersistableCustomer> entity = new HttpEntity<>(testCustomer, getHeader());

        final ResponseEntity<PersistableCustomer> response = testRestTemplate.postForEntity("/api/v1/customer/register", entity, PersistableCustomer.class);
        assertThat(response.getStatusCode(), is(OK));

        // created customer can login

        final ResponseEntity<AuthenticationResponse> loginResponse = testRestTemplate.postForEntity("/api/v1/customer/login", new HttpEntity<>(new AuthenticationRequest(uniqueEmail, "clear123")),
                AuthenticationResponse.class);
        assertThat(loginResponse.getStatusCode(), is(OK));
        assertNotNull(loginResponse.getBody().getToken());

    }

    @Test
    public void privateCustomerLifecycleSupportsListGetUpdatePatchAndDelete() {
        String email = "private-customer+" + System.nanoTime() + "@test.com";
        PersistableCustomer createPayload = customer(email);

        ResponseEntity<ReadableCustomer> createResponse = testRestTemplate.postForEntity(
                "/api/v1/private/customer?store=" + Constants.DEFAULT_STORE,
                new HttpEntity<>(createPayload, getHeader()),
                ReadableCustomer.class);
        assertThat(createResponse.getStatusCode(), is(OK));
        assertNotNull(createResponse.getBody());
        assertNotNull(createResponse.getBody().getId());

        Long customerId = createResponse.getBody().getId();

        ResponseEntity<ReadableCustomer> getResponse = testRestTemplate.exchange(
                "/api/v1/private/customer/" + customerId + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                ReadableCustomer.class);
        assertThat(getResponse.getStatusCode(), is(OK));
        assertNotNull(getResponse.getBody());
        assertThat(getResponse.getBody().getEmailAddress(), is(email));

        ResponseEntity<ReadableCustomerList> listResponse = testRestTemplate.exchange(
                "/api/v1/private/customers?store=" + Constants.DEFAULT_STORE + "&lang=en&page=0&count=5",
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                ReadableCustomerList.class);
        assertThat(listResponse.getStatusCode(), is(OK));
        assertNotNull(listResponse.getBody());
        assertNotNull(listResponse.getBody().getCustomers());
        List<ReadableCustomer> customers = listResponse.getBody().getCustomers();
        boolean found = false;
        for (ReadableCustomer customer : customers) {
            if (email.equals(customer.getEmailAddress())) {
                found = true;
                break;
            }
        }
        assertThat(found, is(true));

        PersistableCustomer addressPatch = new PersistableCustomer();
        Address billingPatch = new Address();
        billingPatch.setFirstName("Private");
        billingPatch.setLastName("Customer");
        billingPatch.setAddress("100 Main Street");
        billingPatch.setCountry("US");
        billingPatch.setCity("Austin");
        billingPatch.setPostalCode("73301");
        addressPatch.setBilling(billingPatch);
        ResponseEntity<String> patchAddressResponse = testRestTemplate.exchange(
                "/api/v1/private/customer/" + customerId + "/address?store=" + Constants.DEFAULT_STORE,
                HttpMethod.PATCH,
                new HttpEntity<>(addressPatch, getHeader()),
                String.class);
        assertThat(patchAddressResponse.getStatusCode(), is(OK));

        PersistableCustomer updatePayload = customer(email);
        updatePayload.setFirstName("PrivateUpdated");
        updatePayload.setLastName("CustomerUpdated");
        ResponseEntity<PersistableCustomer> updateResponse = testRestTemplate.exchange(
                "/api/v1/private/customer/" + customerId + "?store=" + Constants.DEFAULT_STORE,
                HttpMethod.PUT,
                new HttpEntity<>(updatePayload, getHeader()),
                PersistableCustomer.class);
        assertThat(updateResponse.getStatusCode(), is(OK));

        ResponseEntity<String> deleteResponse = testRestTemplate.exchange(
                "/api/v1/private/customer/" + customerId + "?store=" + Constants.DEFAULT_STORE,
                HttpMethod.DELETE,
                new HttpEntity<>(getHeader()),
                String.class);
        assertThat(deleteResponse.getStatusCode(), is(OK));
    }

    @Test
    public void authenticatedCustomerProfileLifecycleSupportsLocalizationAndPatchFlows() {
        String email = "auth-customer+" + System.nanoTime() + "@test.com";

        ResponseEntity<PersistableCustomer> registerResponse = testRestTemplate.postForEntity(
                "/api/v1/customer/register?store=" + Constants.DEFAULT_STORE,
                new HttpEntity<>(customer(email), getHeader()),
                PersistableCustomer.class);
        assertThat(registerResponse.getStatusCode(), is(OK));

        HttpHeaders customerHeaders = customerHeaders(email, "clear123");

        ResponseEntity<ReadableCustomer> profileResponse = testRestTemplate.exchange(
                "/api/v1/auth/customer/profile?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.GET,
                new HttpEntity<>(customerHeaders),
                ReadableCustomer.class);
        assertThat(profileResponse.getStatusCode(), is(OK));
        assertNotNull(profileResponse.getBody());
        assertThat(profileResponse.getBody().getEmailAddress(), is(email));

        PersistableCustomer addressPatch = new PersistableCustomer();
        Address billingPatch = new Address();
        billingPatch.setFirstName("Auth");
        billingPatch.setLastName("User");
        billingPatch.setAddress("200 Pine Street");
        billingPatch.setCountry("US");
        billingPatch.setCity("Seattle");
        billingPatch.setPostalCode("98101");
        addressPatch.setBilling(billingPatch);
        ResponseEntity<String> authAddressPatchResponse = testRestTemplate.exchange(
                "/api/v1/auth/customer/address?store=" + Constants.DEFAULT_STORE,
                HttpMethod.PATCH,
                new HttpEntity<>(addressPatch, customerHeaders),
                String.class);
        assertThat(authAddressPatchResponse.getStatusCode(), is(OK));

        PersistableCustomer profilePatch = customer(email);
        profilePatch.setFirstName("Updated");
        profilePatch.setLastName("AuthCustomer");
        ResponseEntity<PersistableCustomer> authProfilePatchResponse = testRestTemplate.exchange(
                "/api/v1/auth/customer/?store=" + Constants.DEFAULT_STORE,
                HttpMethod.PATCH,
                new HttpEntity<>(profilePatch, customerHeaders),
                PersistableCustomer.class);
        assertThat(authProfilePatchResponse.getStatusCode(), is(OK));
        assertNotNull(authProfilePatchResponse.getBody());

        ResponseEntity<ReadableCustomer> updatedProfileResponse = testRestTemplate.exchange(
                "/api/v1/auth/customer/profile?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.GET,
                new HttpEntity<>(customerHeaders),
                ReadableCustomer.class);
        assertThat(updatedProfileResponse.getStatusCode(), is(OK));
        assertNotNull(updatedProfileResponse.getBody());
        assertThat(updatedProfileResponse.getBody().getEmailAddress(), is(email));
        assertNotNull(updatedProfileResponse.getBody().getBilling());
        assertNotNull(updatedProfileResponse.getBody().getBilling().getCity());

        ResponseEntity<String> deleteAuthCustomerResponse = testRestTemplate.exchange(
                "/api/v1/auth/customer/?store=" + Constants.DEFAULT_STORE,
                HttpMethod.DELETE,
                new HttpEntity<>(customerHeaders),
                String.class);
        assertThat(deleteAuthCustomerResponse.getStatusCode(), is(OK));
    }

    private PersistableCustomer customer(String email) {
        PersistableCustomer customer = new PersistableCustomer();
        customer.setEmailAddress(email);
        customer.setPassword("clear123");
        customer.setRepeatPassword("clear123");
        customer.setGender(CustomerGender.M.name());
        customer.setLanguage("en");
        customer.setStoreCode(Constants.DEFAULT_STORE);
        Address billing = new Address();
        billing.setFirstName("customer");
        billing.setLastName("test");
        billing.setCountry("US");
        billing.setCity("New York");
        customer.setBilling(billing);
        return customer;
    }

    private HttpHeaders customerHeaders(String email, String password) {
        ResponseEntity<AuthenticationResponse> loginResponse = testRestTemplate.postForEntity(
                "/api/v1/customer/login",
                new HttpEntity<>(new AuthenticationRequest(email, password)),
                AuthenticationResponse.class);
        assertThat(loginResponse.getStatusCode(), is(OK));
        assertNotNull(loginResponse.getBody());
        assertNotNull(loginResponse.getBody().getToken());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Bearer " + loginResponse.getBody().getToken());
        return headers;
    }

}
