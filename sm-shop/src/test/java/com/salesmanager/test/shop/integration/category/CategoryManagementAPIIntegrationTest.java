package com.salesmanager.test.shop.integration.category;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.category.CategoryDescription;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.model.catalog.category.ReadableCategoryList;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.catalog.manufacturer.ReadableManufacturer;
import com.salesmanager.shop.model.catalog.product.product.PersistableProduct;
import com.salesmanager.shop.model.catalog.product.product.ProductSpecification;
import com.salesmanager.shop.model.entity.Entity;
import com.salesmanager.shop.model.entity.EntityExists;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class CategoryManagementAPIIntegrationTest extends ServicesTestSupport {
    /**
     * Read - GET a category by id
     *
     * @throws Exception
     */
    @Test
    public void getCategory() throws Exception {
        final HttpEntity<String> httpEntity = new HttpEntity<>(getHeader());

        final ResponseEntity<ReadableCategoryList> response = testRestTemplate.exchange("/api/v1/category".formatted(), HttpMethod.GET,
                httpEntity, ReadableCategoryList.class);
        if (response.getStatusCode() != HttpStatus.OK) {
            throw new Exception(response.toString());
        } else {
            final List<ReadableCategory> categories = response.getBody().getCategories();
            assertNotNull(categories);
        }
    }

    /**
     * Creates - POST a category for a given store
     *
     * @throws Exception
     */

    @Test
    public void postCategory() throws Exception {

        PersistableCategory newCategory = new PersistableCategory();
        newCategory.setCode("javascript");
        newCategory.setSortOrder(1);
        newCategory.setVisible(true);
        newCategory.setDepth(4);

        Category parent = new Category();

        newCategory.setParent(parent);

        CategoryDescription description = new CategoryDescription();
        description.setLanguage("en");
        description.setName("Javascript");
        description.setFriendlyUrl("javascript");
        description.setTitle("Javascript");

        List<CategoryDescription> descriptions = new ArrayList<>();
        descriptions.add(description);

        newCategory.setDescriptions(descriptions);

        final ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        final String json = writer.writeValueAsString(newCategory);

        HttpEntity<String> entity = new HttpEntity<>(json, getHeader());

        ResponseEntity response = testRestTemplate.postForEntity("/api/v1/private/category", entity, PersistableCategory.class);
        PersistableCategory cat = (PersistableCategory) response.getBody();
        assertThat(response.getStatusCode(), is(CREATED));
        assertNotNull(cat.getId());

    }
    
    @Test
    public void putCategory() throws Exception {

        //create
        PersistableCategory newCategory = new PersistableCategory();
        newCategory.setCode("angular");
        newCategory.setSortOrder(1);
        newCategory.setVisible(true);
        newCategory.setDepth(4);


        CategoryDescription description = new CategoryDescription();
        description.setLanguage("en");
        description.setName("angular");
        description.setFriendlyUrl("angular");
        description.setTitle("angular");

        List<CategoryDescription> descriptions = new ArrayList<>();
        descriptions.add(description);

        newCategory.setDescriptions(descriptions);

        final ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        final String json = writer.writeValueAsString(newCategory);

        final HttpEntity<String> entity = new HttpEntity<>(json, getHeader());
        //create category
        final ResponseEntity response = testRestTemplate.postForEntity("/api/v1/private/category", entity, PersistableCategory.class);
        final PersistableCategory cat = (PersistableCategory) response.getBody();
        assertThat(response.getStatusCode(), is(CREATED));
        assertNotNull(cat.getId());
        
        HttpEntity<String> httpEntity = new HttpEntity<>(getHeader());
        
        /**
         * Get by id moving to administration access
         * For public access use friendly url
         */
        
        final ResponseEntity<ReadableCategory> readableQuery = testRestTemplate.exchange(("/api/v1/category/" + description.getFriendlyUrl()).formatted(), HttpMethod.GET,
            httpEntity, ReadableCategory.class);
        
        assertThat(readableQuery.getStatusCode(), is(OK));
        
        ReadableCategory readableCategory = readableQuery.getBody();
        
        newCategory = new PersistableCategory();
        newCategory.setCode("angular");
        newCategory.setVisible(true);
        newCategory.setDepth(4);
        newCategory.setSortOrder(2);
        description = new CategoryDescription();
        description.setLanguage("en");
        description.setName("angular");
        description.setFriendlyUrl("angular");
        description.setTitle("angular");

        descriptions = new ArrayList<>();
        descriptions.add(description);

        newCategory.setDescriptions(descriptions);

        
        HttpEntity<PersistableCategory> requestUpdate = new HttpEntity<>(newCategory, getHeader());
        
        ResponseEntity resp = testRestTemplate.exchange("/api/v1/private/category/" + cat.getId(), HttpMethod.PUT,   requestUpdate, Void.class);
        assertThat(resp.getStatusCode(), is(OK));
        
        //update

    }

    @Test
    public void postComplexCategory() throws Exception {

        /** Dining room **/
        final PersistableCategory dining = new PersistableCategory();
        dining.setCode("diningroom");
        dining.setSortOrder(0);
        dining.setVisible(true);

        CategoryDescription endescription = new CategoryDescription();
        endescription.setLanguage("en");
        endescription.setName("Dining room");
        endescription.setFriendlyUrl("dining-room");
        endescription.setTitle("Dining room");

        CategoryDescription frdescription = new CategoryDescription();
        frdescription.setLanguage("fr");
        frdescription.setName("Salle à manger");
        frdescription.setFriendlyUrl("salle-a-manger");
        frdescription.setTitle("Salle à manger");

        List<CategoryDescription> descriptions = new ArrayList<>();
        descriptions.add(endescription);
        descriptions.add(frdescription);

        dining.setDescriptions(descriptions);

        final Category diningParent = new Category();
        diningParent.setCode(dining.getCode());

        /** armoire **/
        final PersistableCategory armoire = new PersistableCategory();
        armoire.setCode("armoire");
        armoire.setSortOrder(1);
        armoire.setVisible(true);

        armoire.setParent(diningParent);

        endescription = new CategoryDescription();
        endescription.setLanguage("en");
        endescription.setName("Armoires");
        endescription.setFriendlyUrl("armoires");
        endescription.setTitle("Armoires");

        frdescription = new CategoryDescription();
        frdescription.setLanguage("fr");
        frdescription.setName("Armoire");
        frdescription.setFriendlyUrl("armoires");
        frdescription.setTitle("Armoires");

        descriptions = new ArrayList<>();
        descriptions.add(endescription);
        descriptions.add(frdescription);

        armoire.setDescriptions(descriptions);
        dining.getChildren().add(armoire);

        /** benches **/
        final PersistableCategory bench = new PersistableCategory();
        bench.setCode("bench");
        bench.setSortOrder(4);
        bench.setVisible(true);

        bench.setParent(diningParent);

        endescription = new CategoryDescription();
        endescription.setLanguage("en");
        endescription.setName("Benches");
        endescription.setFriendlyUrl("benches");
        endescription.setTitle("Benches");

        frdescription = new CategoryDescription();
        frdescription.setLanguage("fr");
        frdescription.setName("Bancs");
        frdescription.setFriendlyUrl("bancs");
        frdescription.setTitle("Bancs");

        descriptions = new ArrayList<>();
        descriptions.add(endescription);
        descriptions.add(frdescription);

        bench.setDescriptions(descriptions);
        dining.getChildren().add(bench);

        /** Living room **/
        final PersistableCategory living = new PersistableCategory();
        living.setCode("livingroom");
        living.setSortOrder(2);
        living.setVisible(true);

        endescription = new CategoryDescription();
        endescription.setLanguage("en");
        endescription.setName("Living room");
        endescription.setFriendlyUrl("living-room");
        endescription.setTitle("Living room");

        frdescription = new CategoryDescription();
        frdescription.setLanguage("fr");
        frdescription.setName("Salon");
        frdescription.setFriendlyUrl("salon");
        frdescription.setTitle("Salon");

        descriptions = new ArrayList<>();
        descriptions.add(endescription);
        descriptions.add(frdescription);

        living.setDescriptions(descriptions);

        /** lounge **/

        final PersistableCategory lounge = new PersistableCategory();
        lounge.setCode("lounge");
        lounge.setSortOrder(3);
        lounge.setVisible(true);

        final Category livingParent = living;
        lounge.setParent(livingParent);

        endescription = new CategoryDescription();
        endescription.setLanguage("en");
        endescription.setName("Lounge");
        endescription.setFriendlyUrl("lounge");
        endescription.setTitle("Lounge");

        frdescription = new CategoryDescription();
        frdescription.setLanguage("fr");
        frdescription.setName("Divan");
        frdescription.setFriendlyUrl("divan");
        frdescription.setTitle("Divan");

        descriptions = new ArrayList<>();
        descriptions.add(endescription);
        descriptions.add(frdescription);

        lounge.setDescriptions(descriptions);
        living.getChildren().add(lounge);

        final ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        final String json = writer.writeValueAsString(dining);

        //System.out.println(json);

        final HttpEntity<String> entity = new HttpEntity<>(json, getHeader());

        final int sizeBefore = testRestTemplate.exchange("/api/v1/category".formatted(), HttpMethod.GET,
                new HttpEntity<>(getHeader()), ReadableCategoryList.class).getBody().getCategories().size();

        final ResponseEntity response = testRestTemplate.postForEntity("/api/v1/private/category", entity, PersistableCategory.class);

        final PersistableCategory cat = (PersistableCategory) response.getBody();
        assertThat(response.getStatusCode(), is(CREATED));
        assertNotNull(cat.getId());



    }

    @Test
    public void deleteCategory() throws Exception {
        PersistableCategory category = super.category("DELETE-ME");
        HttpEntity<PersistableCategory> createEntity = new HttpEntity<>(category, getHeader());
        ResponseEntity<PersistableCategory> createResponse =
                testRestTemplate.postForEntity("/api/v1/private/category", createEntity, PersistableCategory.class);
        assertThat(createResponse.getStatusCode(), is(CREATED));
        assertNotNull(createResponse.getBody());
        assertNotNull(createResponse.getBody().getId());

        final HttpEntity<String> deleteEntity = new HttpEntity<>(getHeader());
        ResponseEntity<Category> deleteResponse = testRestTemplate.exchange(
                "/api/v1/private/category/" + createResponse.getBody().getId(),
                HttpMethod.DELETE,
                deleteEntity,
                Category.class);
        assertThat(deleteResponse.getStatusCode(), is(OK));
    }

    @Test
    public void manufacturerForItemsInCategory() throws Exception {
      
      ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
      
      //create first manufacturer
      PersistableManufacturer m1 = super.manufacturer("BRAND1");   
      
      String json = writer.writeValueAsString(m1);
      HttpEntity<String> entity = new HttpEntity<>(json, getHeader());

      @SuppressWarnings("rawtypes")
      ResponseEntity response = testRestTemplate.postForEntity("/api/v1/private/manufacturer", entity, PersistableManufacturer.class);
      assertThat(response.getStatusCode(), is(CREATED));

      //create second manufacturer
      PersistableManufacturer m2 = super.manufacturer("BRAND2");
      json = writer.writeValueAsString(m2);
      entity = new HttpEntity<>(json, getHeader());

      response = testRestTemplate.postForEntity("/api/v1/private/manufacturer", entity, PersistableManufacturer.class);
      assertThat(response.getStatusCode(), is(CREATED));
      
      //create category
      PersistableCategory category = super.category("TEST");
      Category cat = new Category();//to be used in product
      cat.setCode("TEST");
      
      json = writer.writeValueAsString(category);
      entity = new HttpEntity<>(json, getHeader());

      @SuppressWarnings("rawtypes")
      ResponseEntity categoryResponse = testRestTemplate.postForEntity("/api/v1/private/category", entity, PersistableCategory.class);
      assertThat(categoryResponse.getStatusCode(), is(CREATED));
      final PersistableCategory persistable = (PersistableCategory) categoryResponse.getBody();
      
      Long id = persistable.getId();
      
      //create first item
      
      PersistableProduct product1 = super.product("PRODUCT1");
      product1.getCategories().add(cat);
      
      
      ProductSpecification specifications = new ProductSpecification();
      specifications.setManufacturer("BRAND1");
      product1.setProductSpecifications(specifications);
      
      json = writer.writeValueAsString(product1);
      entity = new HttpEntity<>(json, getHeader());

      response = testRestTemplate.postForEntity("/api/v1/private/product?store=" + Constants.DEFAULT_STORE, entity, PersistableProduct.class);
      assertThat(response.getStatusCode(), is(CREATED));
            
      //create second item      
      
      PersistableProduct product2 = super.product("PRODUCT2");
      product2.getCategories().add(cat);
      
      
      specifications = new ProductSpecification();
      specifications.setManufacturer("BRAND2");
      product2.setProductSpecifications(specifications);
      
      json = writer.writeValueAsString(product2);
      entity = new HttpEntity<>(json, getHeader());

      response = testRestTemplate.postForEntity("/api/v1/private/product?store=" + Constants.DEFAULT_STORE, entity, PersistableProduct.class);
      assertThat(response.getStatusCode(), is(CREATED));
      
      entity = new HttpEntity<>(getHeader());
            
      //get manufacturers in category
      @SuppressWarnings("rawtypes")
      ResponseEntity<List> manufacturers = testRestTemplate.exchange(("/api/v1/category/" + id + "/manufacturer").formatted(), HttpMethod.GET, entity, List.class);  
      assertThat(manufacturers.getStatusCode(), is(OK));
      
      @SuppressWarnings("unchecked")
      List<ReadableManufacturer> manufacturerList = manufacturers.getBody();

      
      //assertFalse(manufacturerList.isEmpty());
      

      
      
    }
    
    
    /**
     * Test category by name
     * @throws Exception
     */
    
    @Test
    public void getByCategoryFriendlyUrl() throws Exception {
    	
    	ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
    	
    	String categoryName = "running-shoes";
    	String categoryCode = "runningshoes";
    	PersistableCategory category = category(categoryCode,categoryName);
        final String json = writer.writeValueAsString(category);

        HttpEntity<String> entity = new HttpEntity<>(json, getHeader());

        ResponseEntity response = testRestTemplate.postForEntity("/api/v1/private/category", entity, PersistableCategory.class);
        PersistableCategory cat = (PersistableCategory) response.getBody();
        assertThat(response.getStatusCode(), is(CREATED));
        assertNotNull(cat.getId());

        final ResponseEntity<ReadableCategory> readResponse = testRestTemplate.exchange(("/api/v1/category/" + categoryName).formatted(), HttpMethod.GET,
        		entity, ReadableCategory.class);
        if (readResponse.getStatusCode() != HttpStatus.OK) {
            throw new Exception(response.toString());
        } else {
            final ReadableCategory categ = readResponse.getBody();
            assertNotNull(readResponse);
            assertTrue(categoryCode.equals(categ.getCode()));
        }
    }

    @Test
    public void privateCategoryGetUniqueVisiblePatchAndMove() throws Exception {
        PersistableCategory parent = createCategory("cat-parent-" + System.nanoTime());
        PersistableCategory child = createCategory("cat-child-" + System.nanoTime());

        ResponseEntity<ReadableCategory> privateGetResponse = testRestTemplate.exchange(
                "/api/v1/private/category/" + child.getId() + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                ReadableCategory.class);
        assertThat(privateGetResponse.getStatusCode(), is(OK));
        assertNotNull(privateGetResponse.getBody());
        assertThat(privateGetResponse.getBody().getCode(), is(child.getCode()));

        ResponseEntity<EntityExists> uniqueResponse = testRestTemplate.exchange(
                "/api/v1/private/category/unique?store=" + Constants.DEFAULT_STORE + "&lang=en&code=" + child.getCode(),
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                EntityExists.class);
        assertThat(uniqueResponse.getStatusCode(), is(OK));
        assertNotNull(uniqueResponse.getBody());
        assertThat(uniqueResponse.getBody().isExists(), is(true));

        PersistableCategory visibilityPatch = new PersistableCategory();
        visibilityPatch.setVisible(false);
        ResponseEntity<String> visiblePatchResponse = testRestTemplate.exchange(
                "/api/v1/private/category/" + child.getId() + "/visible?store=" + Constants.DEFAULT_STORE,
                HttpMethod.PATCH,
                new HttpEntity<>(visibilityPatch, getHeader()),
                String.class);
        assertThat(visiblePatchResponse.getStatusCode(), is(OK));

        ResponseEntity<String> moveResponse = testRestTemplate.exchange(
                "/api/v1/private/category/" + child.getId() + "/move/" + parent.getId() + "?store=" + Constants.DEFAULT_STORE,
                HttpMethod.PUT,
                new HttpEntity<>(getHeader()),
                String.class);
        assertThat(moveResponse.getStatusCode(), is(OK));
    }

    @Test
    public void categoryListSupportsNamePaginationAndListByProduct() throws Exception {
        String categoryCode = "cat-filter-" + System.nanoTime();
        PersistableCategory category = createCategory(categoryCode);

        ResponseEntity<ReadableCategoryList> pagedListResponse = testRestTemplate.exchange(
                "/api/v1/category?store=" + Constants.DEFAULT_STORE + "&lang=en&name=" + categoryCode + "&page=0&count=1",
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                ReadableCategoryList.class);
        assertThat(pagedListResponse.getStatusCode(), is(OK));
        assertNotNull(pagedListResponse.getBody());
        assertNotNull(pagedListResponse.getBody().getCategories());
        assertTrue(pagedListResponse.getBody().getCategories().size() <= 1);

        PersistableProduct product = super.product("cat-prod-" + System.nanoTime());
        Category productCategory = new Category();
        productCategory.setCode(category.getCode());
        product.getCategories().add(productCategory);
        ProductSpecification specifications = new ProductSpecification();
        specifications.setManufacturer(
                com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer.DEFAULT_MANUFACTURER);
        product.setProductSpecifications(specifications);

        ResponseEntity<Entity> createProductResponse = testRestTemplate.postForEntity(
                "/api/v1/private/product?store=" + Constants.DEFAULT_STORE,
                new HttpEntity<>(product, getHeader()),
                Entity.class);
        assertThat(createProductResponse.getStatusCode(), is(CREATED));
        assertNotNull(createProductResponse.getBody());
        assertNotNull(createProductResponse.getBody().getId());

        ResponseEntity<ReadableCategoryList> byProductResponse = testRestTemplate.exchange(
                "/api/v1/category/product/" + createProductResponse.getBody().getId() + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
                HttpMethod.GET,
                new HttpEntity<>(getHeader()),
                ReadableCategoryList.class);
        assertThat(byProductResponse.getStatusCode(), is(OK));
        assertNotNull(byProductResponse.getBody());
        assertNotNull(byProductResponse.getBody().getCategories());
        assertTrue(byProductResponse.getBody().getCategories().stream().anyMatch(c -> category.getCode().equals(c.getCode())));
    }

    private PersistableCategory createCategory(String code) {
        PersistableCategory category = category(code, code);
        ResponseEntity<PersistableCategory> createResponse = testRestTemplate.postForEntity(
                "/api/v1/private/category?store=" + Constants.DEFAULT_STORE,
                new HttpEntity<>(category, getHeader()),
                PersistableCategory.class);
        assertThat(createResponse.getStatusCode(), is(CREATED));
        assertNotNull(createResponse.getBody());
        assertNotNull(createResponse.getBody().getId());
        return createResponse.getBody();
    }

}
