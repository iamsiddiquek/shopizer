package com.salesmanager.test.shop.integration.search;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.catalog.SearchProductList;
import com.salesmanager.shop.model.catalog.SearchProductRequest;
import com.salesmanager.shop.model.catalog.product.product.PersistableProduct;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
@Ignore
// MIGRATION NOTE: Suppress SpringRunner deprecation warnings because this integration test intentionally stays on the JUnit 4 Spring runner during the Boot 4 migration.
@SuppressWarnings("deprecation")
public class SearchApiIntegrationTest extends ServicesTestSupport {

    @Autowired
    // MIGRATION NOTE: Spring Boot 4 relocated TestRestTemplate to spring-boot-resttestclient without changing test HTTP behavior.
    private TestRestTemplate testRestTemplate;



    /**
     * Add a product then search for it
     * This tests is disabled since it requires Elastic search server started
     *
     * @throws Exception
     */
    //@Test
    @Ignore
    public void searchItem() throws Exception {
    	
    	PersistableProduct product = super.product("TESTPRODUCT");
    	
        final HttpEntity<PersistableProduct> entity = new HttpEntity<>(product, getHeader());

        final ResponseEntity<PersistableProduct> response = testRestTemplate.postForEntity("/api/v1/private/product?store=" + Constants.DEFAULT_STORE, entity, PersistableProduct.class);
        assertThat(response.getStatusCode(), is(CREATED));
        
        SearchProductRequest searchRequest = new SearchProductRequest();
        searchRequest.setQuery("TEST");
        final HttpEntity<SearchProductRequest> searchEntity = new HttpEntity<>(searchRequest, getHeader());
        
        
        final ResponseEntity<SearchProductList> searchResponse = testRestTemplate.postForEntity("/api/v1/search?store=" + Constants.DEFAULT_STORE, searchEntity, SearchProductList.class);
        assertThat(searchResponse.getStatusCode(), is(CREATED));

    }


}
