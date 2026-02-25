package com.salesmanager.test.shop.integration.store;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import jakarta.inject.Inject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.entity.EntityExists;
import com.salesmanager.shop.model.references.PersistableAddress;
import com.salesmanager.shop.model.store.PersistableMerchantStore;
import com.salesmanager.shop.model.store.ReadableMerchantStore;
import com.salesmanager.shop.model.store.ReadableMerchantStoreList;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class MerchantStoreApiIntegrationTest extends ServicesTestSupport {
  
  private static final String TEST_STORE_CODE = "test";
  private static final String CURRENCY = "CAD";
  private static final String DEFAULT_LANGUAGE = "en";
  /**
   * Test get DEFAULT store
   * @throws Exception
   */
  @Test
  public void testGetDefaultStore() throws Exception {
      final HttpEntity<String> httpEntity = new HttpEntity<>(getHeader());

      final ResponseEntity<ReadableMerchantStore> response = testRestTemplate.exchange(("/api/v1/store/" + MerchantStore.DEFAULT_STORE).formatted(), HttpMethod.GET,
              httpEntity, ReadableMerchantStore.class);
      if (response.getStatusCode() != HttpStatus.OK) {
          throw new Exception(response.toString());
      } else {
          final ReadableMerchantStore store = response.getBody();
          assertNotNull(store);
      }
  }
  
  /**
   * Create a new store then delete it
   * @throws Exception
   */
  @Test
  public void testCreateStore() throws Exception {
      
      
      PersistableAddress address = new PersistableAddress();
      address.setAddress("121212 simple address");
      address.setPostalCode("12345");
      address.setCountry("US");
      address.setCity("FT LD");
      address.setStateProvince("FL");

      PersistableMerchantStore createdStore = new PersistableMerchantStore();
      createdStore.setCode(TEST_STORE_CODE);
      createdStore.setCurrency(CURRENCY);
      createdStore.setDefaultLanguage(DEFAULT_LANGUAGE);
      createdStore.setEmail("test@test.com");
      createdStore.setName(TEST_STORE_CODE);
      createdStore.setPhone("444-555-6666");
      createdStore.setSupportedLanguages(Arrays.asList(DEFAULT_LANGUAGE));
      createdStore.setAddress(address);
      
      final HttpEntity<PersistableMerchantStore> httpEntity = new HttpEntity<PersistableMerchantStore>(createdStore, getHeader());

      ResponseEntity<Void> response = testRestTemplate.exchange("/api/v1/private/store".formatted(), HttpMethod.POST, httpEntity, Void.class);

      assertThat(response.getStatusCode(), is(HttpStatus.OK));

  }
  
  
  @Test
  public void testAddAndDeleteStoreLogo() {
      LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<String, Object>();
      parameters.add("file", new org.springframework.core.io.ClassPathResource("image.jpg"));

      HttpHeaders headers = getHeader();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      HttpEntity<LinkedMultiValueMap<String, Object>> entity = new HttpEntity<LinkedMultiValueMap<String, Object>>(parameters, headers);

      ResponseEntity<Void> createResponse = testRestTemplate.exchange(("/api/v1/private/store/" + MerchantStore.DEFAULT_STORE + "/marketing/logo").formatted(), HttpMethod.POST, entity, Void.class);

      // Expect Created
      assertThat(createResponse.getStatusCode(), is(HttpStatus.CREATED));
      
      // now remove logo
      HttpEntity<Void> deleteRequest = new HttpEntity<Void>(getHeader());
      
      ResponseEntity<Void> deleteResponse = testRestTemplate.exchange(("/api/v1/private/store/" + MerchantStore.DEFAULT_STORE + "/marketing/logo").formatted(), HttpMethod.DELETE, deleteRequest, Void.class);

      // Expect Ok
      assertThat(deleteResponse.getStatusCode(), is(HttpStatus.OK));

  }

  @Test
  public void testStoreLocalizationSupportedLanguagesAndPrivateLists() {
      ResponseEntity<ReadableMerchantStore> publicStoreResponse = testRestTemplate.exchange(
              "/api/v1/store/" + MerchantStore.DEFAULT_STORE + "?lang=" + DEFAULT_LANGUAGE,
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              ReadableMerchantStore.class);
      assertThat(publicStoreResponse.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(publicStoreResponse.getBody());
      assertThat(publicStoreResponse.getBody().getCode(), is(MerchantStore.DEFAULT_STORE));
      assertNotNull(publicStoreResponse.getBody().getSupportedLanguages());

      ResponseEntity<List> supportedLanguagesResponse = testRestTemplate.exchange(
              "/api/v1/store/languages?store=" + MerchantStore.DEFAULT_STORE,
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              List.class);
      assertThat(supportedLanguagesResponse.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(supportedLanguagesResponse.getBody());
      assertFalse(supportedLanguagesResponse.getBody().isEmpty());

      ResponseEntity<ReadableMerchantStore> privateStoreResponse = testRestTemplate.exchange(
              "/api/v1/private/store/" + MerchantStore.DEFAULT_STORE + "?lang=" + DEFAULT_LANGUAGE,
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              ReadableMerchantStore.class);
      assertThat(privateStoreResponse.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(privateStoreResponse.getBody());
      assertThat(privateStoreResponse.getBody().getCode(), is(MerchantStore.DEFAULT_STORE));

      ResponseEntity<ReadableMerchantStoreList> privateStoresResponse = testRestTemplate.exchange(
              "/api/v1/private/stores?lang=" + DEFAULT_LANGUAGE + "&page=0&count=2",
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              ReadableMerchantStoreList.class);
      assertThat(privateStoresResponse.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(privateStoresResponse.getBody());
      assertNotNull(privateStoresResponse.getBody().getData());
      assertTrue(privateStoresResponse.getBody().getData().size() <= 2);

      ResponseEntity<List> privateStoreNamesResponse = testRestTemplate.exchange(
              "/api/v1/private/stores/names?lang=" + DEFAULT_LANGUAGE + "&page=0&count=3",
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              List.class);
      assertThat(privateStoreNamesResponse.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(privateStoreNamesResponse.getBody());
  }

  @Test
  public void testStoreUniqueAndMerchantChildStorePagingEndpoints() {
      String code = "store-" + System.nanoTime();

      ResponseEntity<EntityExists> beforeExists = testRestTemplate.exchange(
              "/api/v1/private/store/unique?code=" + code,
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              EntityExists.class);
      assertThat(beforeExists.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(beforeExists.getBody());
      assertThat(beforeExists.getBody().isExists(), is(false));

      PersistableMerchantStore createdStore = store(code);
      ResponseEntity<Void> createResponse = testRestTemplate.exchange(
              "/api/v1/private/store",
              HttpMethod.POST,
              new HttpEntity<>(createdStore, getHeader()),
              Void.class);
      assertThat(createResponse.getStatusCode(), is(HttpStatus.OK));

      ResponseEntity<EntityExists> afterExists = testRestTemplate.exchange(
              "/api/v1/store/unique?code=" + code,
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              EntityExists.class);
      assertThat(afterExists.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(afterExists.getBody());
      assertThat(afterExists.getBody().isExists(), is(true));

      ResponseEntity<ReadableMerchantStoreList> merchantStoresResponse = testRestTemplate.exchange(
              "/api/v1/private/merchant/" + MerchantStore.DEFAULT_STORE + "/stores?lang=" + DEFAULT_LANGUAGE + "&page=0&count=5",
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              ReadableMerchantStoreList.class);
      assertThat(merchantStoresResponse.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(merchantStoresResponse.getBody());
      assertNotNull(merchantStoresResponse.getBody().getData());

      ResponseEntity<ReadableMerchantStoreList> merchantChildrenResponse = testRestTemplate.exchange(
              "/api/v1/private/merchant/" + MerchantStore.DEFAULT_STORE + "/children?lang=" + DEFAULT_LANGUAGE + "&page=0&count=5",
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              ReadableMerchantStoreList.class);
      assertThat(merchantChildrenResponse.getStatusCode(), is(HttpStatus.OK));
      assertNotNull(merchantChildrenResponse.getBody());
      assertNotNull(merchantChildrenResponse.getBody().getData());
  }

  @Test
  public void testGetUnknownStoreReturnsNotFound() {
      ResponseEntity<String> response = testRestTemplate.exchange(
              "/api/v1/store/unknown-store-" + System.nanoTime() + "?lang=" + DEFAULT_LANGUAGE,
              HttpMethod.GET,
              new HttpEntity<>(getHeader()),
              String.class);
      assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
  }

  private PersistableMerchantStore store(String code) {
      PersistableAddress address = new PersistableAddress();
      address.setAddress("121212 simple address");
      address.setPostalCode("12345");
      address.setCountry("US");
      address.setCity("FT LD");
      address.setStateProvince("FL");

      PersistableMerchantStore createdStore = new PersistableMerchantStore();
      createdStore.setCode(code);
      createdStore.setCurrency(CURRENCY);
      createdStore.setDefaultLanguage(DEFAULT_LANGUAGE);
      createdStore.setEmail(code + "@test.com");
      createdStore.setName(code);
      createdStore.setPhone("444-555-6666");
      createdStore.setSupportedLanguages(Arrays.asList(DEFAULT_LANGUAGE));
      createdStore.setAddress(address);
      return createdStore;
  }

  
  
}
