package com.salesmanager.test.shop.integration.product;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionType;
import com.salesmanager.shop.application.ShopApplication;
import com.salesmanager.shop.model.catalog.product.LightPersistableProduct;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.category.CategoryDescription;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.product.PersistableProductReview;
import com.salesmanager.shop.model.catalog.product.ProductDescription;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.catalog.product.ReadableProductList;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOptionValue;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionDescription;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription;
import com.salesmanager.shop.model.catalog.product.attribute.api.PersistableProductOptionEntity;
import com.salesmanager.shop.model.catalog.product.attribute.api.ReadableProductOptionEntity;
import com.salesmanager.shop.model.catalog.product.attribute.api.ReadableProductOptionList;
import com.salesmanager.shop.model.catalog.product.attribute.api.ReadableProductOptionValue;
import com.salesmanager.shop.model.catalog.product.attribute.api.ReadableProductOptionValueList;
import com.salesmanager.shop.model.catalog.product.product.PersistableProduct;
import com.salesmanager.shop.model.catalog.product.product.ProductSpecification;
import com.salesmanager.shop.model.catalog.product.product.definition.PersistableProductDefinition;
import com.salesmanager.shop.model.catalog.product.product.definition.ReadableProductDefinition;
import com.salesmanager.shop.model.customer.PersistableCustomer;
import com.salesmanager.shop.model.customer.address.Address;
import com.salesmanager.shop.model.entity.Entity;
import com.salesmanager.shop.model.entity.EntityExists;
import com.salesmanager.shop.store.security.AuthenticationResponse;
import com.salesmanager.test.shop.common.ServicesTestSupport;

@SpringBootTest(classes = ShopApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@RunWith(SpringRunner.class)
public class ProductManagementAPIIntegrationTest extends ServicesTestSupport {

	@Test
	public void createProductWithCategory() throws Exception {


		final PersistableCategory newCategory = new PersistableCategory();
		newCategory.setCode("test-cat");
		newCategory.setSortOrder(1);
		newCategory.setVisible(true);
		newCategory.setDepth(4);

		final Category parent = new Category();

		newCategory.setParent(parent);

		final CategoryDescription description = new CategoryDescription();
		description.setLanguage("en");
		description.setName("test-cat");
		description.setFriendlyUrl("test-cat");
		description.setTitle("test-cat");

		final List<CategoryDescription> descriptions = new ArrayList<>();
		descriptions.add(description);

		newCategory.setDescriptions(descriptions);

		final HttpEntity<PersistableCategory> categoryEntity = new HttpEntity<>(newCategory, getHeader());

		final ResponseEntity<PersistableCategory> categoryResponse = testRestTemplate.postForEntity(
				"/api/v1/private/category?store=" + Constants.DEFAULT_STORE, categoryEntity, PersistableCategory.class);
		final PersistableCategory cat = categoryResponse.getBody();
		assertThat(categoryResponse.getStatusCode(), is(CREATED));
		assertNotNull(cat.getId());

		final PersistableProduct product = super.product("PRODUCT12");
		final ArrayList<Category> categories = new ArrayList<>();
		categories.add(cat);
		product.setCategories(categories);
		ProductSpecification specifications = new ProductSpecification();
		specifications.setManufacturer(
				com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer.DEFAULT_MANUFACTURER);
		product.setProductSpecifications(specifications);
		product.setPrice(BigDecimal.TEN);
		product.setSku("123ABC");
		final HttpEntity<PersistableProduct> entity = new HttpEntity<>(product, getHeader());

		final ResponseEntity<PersistableProduct> response = testRestTemplate.postForEntity(
				"/api/v1/private/product?store=" + Constants.DEFAULT_STORE, entity, PersistableProduct.class);
		assertThat(response.getStatusCode(), is(CREATED));
	}

	/**
	 * Creates a ProductReview requires an existing Customer and an existing
	 * Product
	 *
	 * @throws Exception
	 */
	@Test
	public void createProductReview() throws Exception {

		ReadableProduct product = sampleProduct(uniqueCode("review-product-"));
		assertNotNull(product);
		assertNotNull(product.getId());

		PersistableCustomer customer = new PersistableCustomer();
		String customerEmail = uniqueCode("review-customer-") + "@test.com";
		customer.setEmailAddress(customerEmail);
		customer.setPassword("clear123");
		customer.setGender(CustomerGender.M.name());
		customer.setLanguage("en");
		customer.setStoreCode(Constants.DEFAULT_STORE);
		Address billing = new Address();
		billing.setFirstName("review");
		billing.setLastName("customer");
		billing.setCountry("US");
		customer.setBilling(billing);
		ResponseEntity<AuthenticationResponse> registrationResponse = testRestTemplate.postForEntity(
				"/api/v1/customer/register",
				new HttpEntity<>(customer, getHeader()),
				AuthenticationResponse.class);
		assertThat(registrationResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(registrationResponse.getBody());
		assertNotNull(registrationResponse.getBody().getId());

		final PersistableProductReview review = new PersistableProductReview();
		review.setCustomerId(registrationResponse.getBody().getId());
		review.setProductId(product.getId());
		review.setLanguage("en");
		review.setRating(2D);
		review.setDescription("Boot 4 migrated product review integration test");
		review.setDate("2026-02-25");

		final HttpEntity<PersistableProductReview> entity = new HttpEntity<>(review, getHeader());
		final ResponseEntity<PersistableProductReview> response =
				testRestTemplate.postForEntity(
						"/api/v1/private/products/" + product.getId() + "/reviews?store=" + Constants.DEFAULT_STORE,
						entity,
						PersistableProductReview.class);

		assertThat(response.getStatusCode(), is(CREATED));
		assertNotNull(response.getBody());
		assertThat(response.getBody().getProductId(), is(product.getId()));

	}

	/**
	 * Creates a product option value that can be used to create a product
	 * attribute when creating a new product
	 *
	 * @throws Exception
	 */
	@Test
	public void createOptionValue() throws Exception {

		final ProductOptionValueDescription description = new ProductOptionValueDescription();
		description.setLanguage("en");
		description.setName("Red");

		final List<ProductOptionValueDescription> descriptions = new ArrayList<>();
		descriptions.add(description);

		final PersistableProductOptionValue optionValue = new PersistableProductOptionValue();
		optionValue.setOrder(1);
		optionValue.setCode(uniqueCode("colorred"));
		optionValue.setDescriptions(descriptions);
		final HttpEntity<PersistableProductOptionValue> entity = new HttpEntity<>(optionValue, getHeader());

		final ResponseEntity<ReadableProductOptionValue> response = testRestTemplate.postForEntity(
				"/api/v1/private/product/option/value?store=" + Constants.DEFAULT_STORE,
				entity,
				ReadableProductOptionValue.class);

		assertThat(response.getStatusCode(), is(CREATED));
		assertNotNull(response.getBody());
		assertNotNull(response.getBody().getId());
		assertThat(response.getBody().getCode(), is(optionValue.getCode()));

	}

	/**
	 * Creates a new ProductOption
	 *
	 * @throws Exception
	 */
	@Test
	public void createOption() throws Exception {

		final ProductOptionDescription description = new ProductOptionDescription();
		description.setLanguage("en");
		description.setName("Color");

		final List<ProductOptionDescription> descriptions = new ArrayList<>();
		descriptions.add(description);

		final PersistableProductOptionEntity option = new PersistableProductOptionEntity();
		option.setOrder(1);
		option.setCode(uniqueCode("color"));
		option.setType(ProductOptionType.Select.name());
		option.setDescriptions(descriptions);
		final HttpEntity<PersistableProductOptionEntity> entity = new HttpEntity<>(option, getHeader());

		final ResponseEntity<ReadableProductOptionEntity> response = testRestTemplate.postForEntity(
				"/api/v1/private/product/option?store=" + Constants.DEFAULT_STORE,
				entity,
				ReadableProductOptionEntity.class);

		assertThat(response.getStatusCode(), is(CREATED));
		assertNotNull(response.getBody());
		assertNotNull(response.getBody().getId());
		assertThat(response.getBody().getCode(), is(option.getCode()));

	}

	@Test
	public void getProducts() throws Exception {
		ReadableProduct created = sampleProduct(uniqueCode("list-product-"));
		assertNotNull(created);
		assertNotNull(created.getSku());

		final HttpEntity<String> httpEntity = new HttpEntity<>(getHeader());
		final ResponseEntity<ReadableProductList> response =
				testRestTemplate.exchange(
						"/api/v2/products?store=" + Constants.DEFAULT_STORE + "&sku=" + created.getSku() + "&count=10",
						HttpMethod.GET,
						httpEntity,
						ReadableProductList.class);

		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(response.getBody());
		assertNotNull(response.getBody().getProducts());
		assertThat(
				response.getBody().getProducts().stream().anyMatch(p -> created.getSku().equals(p.getSku())),
				is(true));
	}

	@Test
	public void getProductsReturnsNoMatchForUnknownSku() throws Exception {
		String sku = uniqueCode("missing-sku-");

		ResponseEntity<ReadableProductList> response = testRestTemplate.exchange(
				"/api/v2/products?store=" + Constants.DEFAULT_STORE + "&sku=" + sku + "&count=10",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductList.class);

		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(response.getBody());
		assertNotNull(response.getBody().getProducts());
		assertThat(response.getBody().getProducts().stream().anyMatch(p -> sku.equals(p.getSku())), is(false));
	}

	@Test
	public void optionLifecycleAndUniqueChecks() throws Exception {
		String code = uniqueCode("opt-");

		ResponseEntity<EntityExists> beforeCreateUnique = testRestTemplate.exchange(
				"/api/v1/private/product/option/unique?store=" + Constants.DEFAULT_STORE + "&code=" + code,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(beforeCreateUnique.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(beforeCreateUnique.getBody());
		assertThat(beforeCreateUnique.getBody().isExists(), is(false));

		ProductOptionDescription createDescription = new ProductOptionDescription();
		createDescription.setLanguage("en");
		createDescription.setName("Lifecycle Option");
		PersistableProductOptionEntity createPayload = new PersistableProductOptionEntity();
		createPayload.setCode(code);
		createPayload.setOrder(1);
		createPayload.setType(ProductOptionType.Select.name());
		createPayload.getDescriptions().add(createDescription);

		ResponseEntity<ReadableProductOptionEntity> createResponse = testRestTemplate.postForEntity(
				"/api/v1/private/product/option?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(createPayload, getHeader()),
				ReadableProductOptionEntity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());
		Long optionId = createResponse.getBody().getId();

		ResponseEntity<EntityExists> afterCreateUnique = testRestTemplate.exchange(
				"/api/v1/private/product/option/unique?store=" + Constants.DEFAULT_STORE + "&code=" + code,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(afterCreateUnique.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(afterCreateUnique.getBody());
		assertThat(afterCreateUnique.getBody().isExists(), is(true));

		ResponseEntity<ReadableProductOptionEntity> readResponse = testRestTemplate.exchange(
				"/api/v1/private/product/option/" + optionId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionEntity.class);
		assertThat(readResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(readResponse.getBody());
		assertThat(readResponse.getBody().getCode(), is(code));

		ProductOptionDescription updateDescription = new ProductOptionDescription();
		updateDescription.setLanguage("en");
		updateDescription.setName("Lifecycle Option Updated");
		PersistableProductOptionEntity updatePayload = new PersistableProductOptionEntity();
		updatePayload.setCode(code);
		updatePayload.setOrder(7);
		updatePayload.setType(ProductOptionType.Select.name());
		updatePayload.getDescriptions().add(updateDescription);
		ResponseEntity<Void> updateResponse = testRestTemplate.exchange(
				"/api/v1/private/product/option/" + optionId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.PUT,
				new HttpEntity<>(updatePayload, getHeader()),
				Void.class);
		assertThat(updateResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<ReadableProductOptionEntity> readAfterUpdate = testRestTemplate.exchange(
				"/api/v1/private/product/option/" + optionId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionEntity.class);
		assertThat(readAfterUpdate.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(readAfterUpdate.getBody());
		assertThat(readAfterUpdate.getBody().getCode(), is(code));

		ResponseEntity<ReadableProductOptionList> listResponse = testRestTemplate.exchange(
				"/api/v1/private/product/options?store=" + Constants.DEFAULT_STORE + "&count=200",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionList.class);
		assertThat(listResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(listResponse.getBody());
		assertNotNull(listResponse.getBody().getOptions());
		assertThat(listResponse.getBody().getOptions().stream().anyMatch(o -> code.equals(o.getCode())), is(true));

		ResponseEntity<Void> deleteResponse = testRestTemplate.exchange(
				"/api/v1/private/product/option/" + optionId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.DELETE,
				new HttpEntity<>(getHeader()),
				Void.class);
		assertThat(deleteResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<EntityExists> afterDeleteUnique = testRestTemplate.exchange(
				"/api/v1/private/product/option/unique?store=" + Constants.DEFAULT_STORE + "&code=" + code,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(afterDeleteUnique.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(afterDeleteUnique.getBody());
		assertThat(afterDeleteUnique.getBody().isExists(), is(false));
	}

	@Test
	public void optionValueLifecycleAndUniqueChecks() throws Exception {
		String code = uniqueCode("optval-");

		ResponseEntity<EntityExists> beforeCreateUnique = testRestTemplate.exchange(
				"/api/v1/private/product/option/value/unique?store=" + Constants.DEFAULT_STORE + "&code=" + code,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(beforeCreateUnique.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(beforeCreateUnique.getBody());
		assertThat(beforeCreateUnique.getBody().isExists(), is(false));

		ProductOptionValueDescription createDescription = new ProductOptionValueDescription();
		createDescription.setLanguage("en");
		createDescription.setName("Lifecycle Option Value");
		PersistableProductOptionValue createPayload = new PersistableProductOptionValue();
		createPayload.setCode(code);
		createPayload.setOrder(1);
		createPayload.setDescriptions(List.of(createDescription));

		ResponseEntity<ReadableProductOptionValue> createResponse = testRestTemplate.postForEntity(
				"/api/v1/private/product/option/value?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(createPayload, getHeader()),
				ReadableProductOptionValue.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());
		Long optionValueId = createResponse.getBody().getId();

		ResponseEntity<EntityExists> afterCreateUnique = testRestTemplate.exchange(
				"/api/v1/private/product/option/value/unique?store=" + Constants.DEFAULT_STORE + "&code=" + code,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(afterCreateUnique.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(afterCreateUnique.getBody());
		assertThat(afterCreateUnique.getBody().isExists(), is(true));

		ResponseEntity<ReadableProductOptionValue> readResponse = testRestTemplate.exchange(
				"/api/v1/private/product/option/value/" + optionValueId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionValue.class);
		assertThat(readResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(readResponse.getBody());
		assertThat(readResponse.getBody().getCode(), is(code));

		ProductOptionValueDescription updateDescription = new ProductOptionValueDescription();
		updateDescription.setLanguage("en");
		updateDescription.setName("Lifecycle Option Value Updated");
		PersistableProductOptionValue updatePayload = new PersistableProductOptionValue();
		updatePayload.setCode(code);
		updatePayload.setOrder(8);
		updatePayload.setDescriptions(List.of(updateDescription));
		ResponseEntity<Void> updateResponse = testRestTemplate.exchange(
				"/api/v1/private/product/option/value/" + optionValueId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.PUT,
				new HttpEntity<>(updatePayload, getHeader()),
				Void.class);
		assertThat(updateResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<ReadableProductOptionValue> readAfterUpdate = testRestTemplate.exchange(
				"/api/v1/private/product/option/value/" + optionValueId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionValue.class);
		assertThat(readAfterUpdate.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(readAfterUpdate.getBody());
		assertThat(readAfterUpdate.getBody().getCode(), is(code));

		ResponseEntity<ReadableProductOptionValueList> listResponse = testRestTemplate.exchange(
				"/api/v1/private/product/options/values?store=" + Constants.DEFAULT_STORE + "&count=200",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionValueList.class);
		assertThat(listResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(listResponse.getBody());
		assertNotNull(listResponse.getBody().getOptionValues());
		assertThat(
				listResponse.getBody().getOptionValues().stream().anyMatch(v -> code.equals(v.getCode())),
				is(true));

		ResponseEntity<Void> deleteResponse = testRestTemplate.exchange(
				"/api/v1/private/product/option/value/" + optionValueId + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.DELETE,
				new HttpEntity<>(getHeader()),
				Void.class);
		assertThat(deleteResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<EntityExists> afterDeleteUnique = testRestTemplate.exchange(
				"/api/v1/private/product/option/value/unique?store=" + Constants.DEFAULT_STORE + "&code=" + code,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(afterDeleteUnique.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(afterDeleteUnique.getBody());
		assertThat(afterDeleteUnique.getBody().isExists(), is(false));
	}

	@Test
	public void putProduct() throws Exception {
		String sku = uniqueCode("put-product-");
		PersistableCategory category = createPrivateCategory(uniqueCode("put-category-"));
		PersistableProductDefinition createPayload = v2Definition(sku, category, 3, "Put Product Initial");

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(createPayload, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());

		PersistableProductDefinition updatePayload = v2Definition(sku, category, 99, "Put Product Updated");
		ResponseEntity<Void> updateResponse = testRestTemplate.exchange(
				"/api/v2/private/product/" + createResponse.getBody().getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.PUT,
				new HttpEntity<>(updatePayload, getHeader()),
				Void.class);
		assertThat(updateResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<ReadableProductDefinition> readResponse = testRestTemplate.exchange(
				"/api/v2/private/product/" + createResponse.getBody().getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductDefinition.class);
		assertThat(readResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(readResponse.getBody());
		assertThat(readResponse.getBody().getSku(), is(sku));
		assertThat(readResponse.getBody().getSortOrder(), is(99));

	}

	@Test
	public void postProduct() throws Exception {
		String sku = uniqueCode("post-product-");
		PersistableCategory category = createPrivateCategory(uniqueCode("post-category-"));
		PersistableProductDefinition createPayload = v2Definition(sku, category, 5, "Post Product");

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(createPayload, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());

		ResponseEntity<ReadableProduct> readResponse = testRestTemplate.exchange(
				"/api/v2/product/" + sku + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProduct.class);
		assertThat(readResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(readResponse.getBody());
		assertThat(readResponse.getBody().getSku(), is(sku));

	}

	@Test
	public void patchProductInventoryBySku() throws Exception {
		String sku = uniqueCode("patch-product-");
		PersistableCategory category = createPrivateCategory(uniqueCode("patch-category-"));
		PersistableProductDefinition createPayload = v2Definition(sku, category, 4, "Patch Product");

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(createPayload, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());

		LightPersistableProduct patchPayload = new LightPersistableProduct();
		patchPayload.setQuantity(42);
		patchPayload.setPrice("19.99");
		patchPayload.setAvailable(true);
		patchPayload.setProductShipeable(true);

		ResponseEntity<Void> patchResponse = testRestTemplate.exchange(
				"/api/v2/private/product/" + sku + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.PATCH,
				new HttpEntity<>(patchPayload, getHeader()),
				Void.class);
		assertThat(patchResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<ReadableProductDefinition> readResponse = testRestTemplate.exchange(
				"/api/v2/private/product/" + createResponse.getBody().getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductDefinition.class);
		assertThat(readResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(readResponse.getBody());
		assertNotNull(readResponse.getBody().getInventory());
		assertThat(readResponse.getBody().getInventory().getQuantity(), is(42));
	}

	@Test
	public void deleteProductRemovesPublicReadBySku() throws Exception {
		String sku = uniqueCode("delete-product-");
		PersistableCategory category = createPrivateCategory(uniqueCode("delete-category-"));
		PersistableProductDefinition createPayload = v2Definition(sku, category, 6, "Delete Product");

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(createPayload, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());

		ResponseEntity<Void> deleteResponse = testRestTemplate.exchange(
				"/api/v2/private/product/" + createResponse.getBody().getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.DELETE,
				new HttpEntity<>(getHeader()),
				Void.class);
		assertThat(deleteResponse.getStatusCode(), is(HttpStatus.OK));

		ResponseEntity<ReadableProduct> readDeletedResponse = testRestTemplate.exchange(
				"/api/v2/product/" + sku + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProduct.class);
		assertThat(readDeletedResponse.getStatusCode().is4xxClientError(), is(true));
	}

	@Test
	public void getMissingProductBySkuReturnsClientError() throws Exception {
		ResponseEntity<ReadableProduct> response = testRestTemplate.exchange(
				"/api/v2/product/" + uniqueCode("unknown-product-") + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProduct.class);
		assertThat(response.getStatusCode().is4xxClientError(), is(true));
	}

	@Test
	public void createDuplicateProductSkuReturnsClientError() throws Exception {
		String sku = uniqueCode("dup-product-");
		PersistableCategory category = createPrivateCategory(uniqueCode("dup-category-"));
		PersistableProductDefinition createPayload = v2Definition(sku, category, 8, "Duplicate SKU Product");

		ResponseEntity<Entity> firstCreate = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(createPayload, getHeader()),
				Entity.class);
		assertThat(firstCreate.getStatusCode(), is(CREATED));
		assertNotNull(firstCreate.getBody());
		assertNotNull(firstCreate.getBody().getId());

		ResponseEntity<String> duplicateCreate = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(v2Definition(sku, category, 9, "Duplicate SKU Product 2"), getHeader()),
				String.class);
		assertThat(duplicateCreate.getStatusCode().is4xxClientError(), is(true));
	}

	@Test
	public void getMissingPrivateProductByIdReturnsNotFound() throws Exception {
		ResponseEntity<ReadableProductDefinition> response = testRestTemplate.exchange(
				"/api/v2/private/product/" + Long.MAX_VALUE + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductDefinition.class);
		assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
	}

	@Test
	public void patchMissingProductBySkuReturnsClientError() throws Exception {
		LightPersistableProduct patchPayload = new LightPersistableProduct();
		patchPayload.setQuantity(1);
		patchPayload.setPrice("5.00");
		patchPayload.setAvailable(true);
		patchPayload.setProductShipeable(true);

		ResponseEntity<String> patchResponse = testRestTemplate.exchange(
				"/api/v2/private/product/" + uniqueCode("missing-patch-") + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.PATCH,
				new HttpEntity<>(patchPayload, getHeader()),
				String.class);
		assertThat(patchResponse.getStatusCode().is4xxClientError(), is(true));
	}

	@Test
	public void createDuplicateOptionCodeReturnsConflict() throws Exception {
		String code = uniqueCode("dup-opt-");

		ProductOptionDescription description = new ProductOptionDescription();
		description.setLanguage("en");
		description.setName("Duplicate Option");

		PersistableProductOptionEntity payload = new PersistableProductOptionEntity();
		payload.setOrder(1);
		payload.setCode(code);
		payload.setType(ProductOptionType.Select.name());
		payload.getDescriptions().add(description);

		ResponseEntity<ReadableProductOptionEntity> firstCreate = testRestTemplate.postForEntity(
				"/api/v1/private/product/option?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(payload, getHeader()),
				ReadableProductOptionEntity.class);
		assertThat(firstCreate.getStatusCode(), is(CREATED));

		ResponseEntity<String> duplicateCreate = testRestTemplate.postForEntity(
				"/api/v1/private/product/option?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(payload, getHeader()),
				String.class);
		assertThat(duplicateCreate.getStatusCode(), is(HttpStatus.CONFLICT));
	}

	@Test
	public void patchProductInventoryMalformedJsonReturnsBadRequest() throws Exception {
		String sku = uniqueCode("patch-bad-json-");
		PersistableCategory category = createPrivateCategory(uniqueCode("patch-bad-json-cat-"));

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(v2Definition(sku, category, 2, "Patch Bad Json"), getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));

		HttpHeaders headers = getHeader();
		headers.setContentType(MediaType.APPLICATION_JSON);
		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v2/private/product/" + sku + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.PATCH,
				new HttpEntity<>("{\"quantity\":}", headers),
				String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
	}

	@Test
	public void getProductByFriendlyUrlReturnsProduct() throws Exception {
		String sku = uniqueCode("v1-friendly-");
		PersistableCategory category = createPrivateCategory(uniqueCode("v1-friendly-cat-"));
		PersistableProductDefinition product = v2Definition(sku, category, 1, "Friendly Product " + System.nanoTime());
		String friendlyUrl = uniqueCode("friendly-url-");
		product.getDescriptions().get(0).setFriendlyUrl(friendlyUrl);

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));

		ResponseEntity<ReadableProduct> response = testRestTemplate.exchange(
				"/api/v1/product/friendly/" + friendlyUrl + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				HttpEntity.EMPTY,
				ReadableProduct.class);
		assertThat(response.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(response.getBody());
		assertThat(response.getBody().getSku(), is(sku));
		assertNotNull(response.getBody().getDescription());
		assertThat(response.getBody().getDescription().getFriendlyUrl(), is(friendlyUrl));
	}

	@Test
	public void getMissingProductByFriendlyUrlReturnsNotFound() throws Exception {
		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/product/friendly/" + uniqueCode("missing-friendly-") + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.GET,
				HttpEntity.EMPTY,
				String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.NOT_FOUND));
	}

	@Test
	public void productUniqueEndpointReportsFalseThenTrue() throws Exception {
		String sku = uniqueCode("v1-unique-");

		ResponseEntity<EntityExists> before = testRestTemplate.exchange(
				"/api/v1/private/product/unique?store=" + Constants.DEFAULT_STORE + "&code=" + sku,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(before.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(before.getBody());
		assertThat(before.getBody().isExists(), is(false));

		PersistableCategory category = createPrivateCategory(uniqueCode("v1-unique-cat-"));
		PersistableProductDefinition product = v2Definition(sku, category, 2, "Unique Product " + System.nanoTime());
		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));

		ResponseEntity<EntityExists> after = testRestTemplate.exchange(
				"/api/v1/private/product/unique?store=" + Constants.DEFAULT_STORE + "&code=" + sku,
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				EntityExists.class);
		assertThat(after.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(after.getBody());
		assertThat(after.getBody().isExists(), is(true));
	}

	@Test
	public void v1ProductListFiltersBySlugAndSku() throws Exception {
		String sku = uniqueCode("v1-list-");
		PersistableCategory baseCategory = createPrivateCategory(uniqueCode("v1-list-base-"));
		String productName = "ListProduct" + System.nanoTime();
		PersistableProductDefinition product = v2Definition(sku, baseCategory, 3, productName);

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());

		PersistableCategory filterCategory = createPrivateCategory(uniqueCode("v1-list-filter-"));
		ResponseEntity<String> addCategoryResponse = testRestTemplate.exchange(
				"/api/v1/private/product/" + createResponse.getBody().getId()
						+ "/category/" + filterCategory.getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.POST,
				new HttpEntity<>(getHeader()),
				String.class);
		assertThat(addCategoryResponse.getStatusCode(), is(CREATED));

		String slug = filterCategory.getDescriptions().get(0).getFriendlyUrl();
		ResponseEntity<ReadableProductList> listResponse = testRestTemplate.exchange(
				"/api/v1/products?store=" + Constants.DEFAULT_STORE
						+ "&lang=en"
						+ "&slug=" + slug
						+ "&category=" + filterCategory.getId()
						+ "&sku=" + sku
						+ "&name=" + productName
						+ "&page=0"
						+ "&count=20",
				HttpMethod.GET,
				HttpEntity.EMPTY,
				ReadableProductList.class);
		assertThat(listResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(listResponse.getBody());
		assertNotNull(listResponse.getBody().getProducts());
		boolean found = false;
		for (ReadableProduct listed : listResponse.getBody().getProducts()) {
			if (sku.equals(listed.getSku())) {
				found = true;
				break;
			}
		}
		assertThat(found, is(true));
	}

	@Test
	public void addAndRemoveProductCategoryAssociation() throws Exception {
		String sku = uniqueCode("v1-cat-link-");
		PersistableCategory baseCategory = createPrivateCategory(uniqueCode("v1-cat-link-base-"));
		PersistableProductDefinition product = v2Definition(sku, baseCategory, 4, "Category Link " + System.nanoTime());

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());

		PersistableCategory extraCategory = createPrivateCategory(uniqueCode("v1-cat-link-extra-"));

		ResponseEntity<String> addResponse = testRestTemplate.exchange(
				"/api/v1/private/product/" + createResponse.getBody().getId()
						+ "/category/" + extraCategory.getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.POST,
				new HttpEntity<>(getHeader()),
				String.class);
		assertThat(addResponse.getStatusCode(), is(CREATED));

		ResponseEntity<String> removeResponse = testRestTemplate.exchange(
				"/api/v1/private/product/" + createResponse.getBody().getId()
						+ "/category/" + extraCategory.getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.DELETE,
				new HttpEntity<>(getHeader()),
				String.class);
		assertThat(removeResponse.getStatusCode(), is(HttpStatus.OK));
	}

	@Test
	public void addProductToCategoryWithMissingProductReturnsBadRequest() throws Exception {
		PersistableCategory category = createPrivateCategory(uniqueCode("v1-missing-product-cat-"));

		ResponseEntity<String> response = testRestTemplate.exchange(
				"/api/v1/private/product/" + Long.MAX_VALUE
						+ "/category/" + category.getId() + "?store=" + Constants.DEFAULT_STORE,
				HttpMethod.POST,
				new HttpEntity<>(getHeader()),
				String.class);
		assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
	}

	@Test
	public void getProductByFriendlyUrlV2ReturnsProduct() throws Exception {
		String sku = uniqueCode("v2-friendly-");
		PersistableCategory category = createPrivateCategory(uniqueCode("v2-friendly-cat-"));
		PersistableProductDefinition product = v2Definition(sku, category, 5, "V2 Friendly " + System.nanoTime());
		String friendlyUrl = uniqueCode("v2-friendly-url-");
		product.getDescriptions().get(0).setFriendlyUrl(friendlyUrl);

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));

		ResponseEntity<ReadableProduct> getResponse = testRestTemplate.exchange(
				"/api/v2/product/friendly/" + friendlyUrl + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
				HttpMethod.GET,
				HttpEntity.EMPTY,
				ReadableProduct.class);
		assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(getResponse.getBody());
		assertThat(getResponse.getBody().getSku(), is(sku));
	}

	@Test
	public void listProductsByCategoryFriendlyUrlV2SupportsPaginationAndLocalization() throws Exception {
		String sku = uniqueCode("v2-cat-list-");
		PersistableCategory category = createPrivateCategory(uniqueCode("v2-cat-list-cat-"));
		PersistableProductDefinition product = v2Definition(sku, category, 6, "V2 Category List " + System.nanoTime());

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));

		String slug = category.getDescriptions().get(0).getFriendlyUrl();
		ResponseEntity<ReadableProductList> listResponse = testRestTemplate.exchange(
				"/api/v2/products/category/" + slug + "?store=" + Constants.DEFAULT_STORE + "&lang=en&page=0&count=5",
				HttpMethod.GET,
				HttpEntity.EMPTY,
				ReadableProductList.class);
		assertThat(listResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(listResponse.getBody());
		assertNotNull(listResponse.getBody().getProducts());
		boolean found = false;
		for (ReadableProduct listed : listResponse.getBody().getProducts()) {
			if (sku.equals(listed.getSku())) {
				found = true;
				break;
			}
		}
		assertThat(found, is(true));
	}

	@Test
	public void v2ProductsListFiltersByNameAndSku() throws Exception {
		String sku = uniqueCode("v2-filter-");
		String name = "V2 Filter Name " + System.nanoTime();
		PersistableCategory category = createPrivateCategory(uniqueCode("v2-filter-cat-"));
		PersistableProductDefinition product = v2Definition(sku, category, 7, name);

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));

		ResponseEntity<ReadableProductList> listResponse = testRestTemplate.exchange(
				"/api/v2/products?store=" + Constants.DEFAULT_STORE
						+ "&lang=en"
						+ "&sku=" + sku
						+ "&name=" + name
						+ "&count=5",
				HttpMethod.GET,
				HttpEntity.EMPTY,
				ReadableProductList.class);
		assertThat(listResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(listResponse.getBody());
		assertNotNull(listResponse.getBody().getProducts());
		assertThat(listResponse.getBody().getProducts().stream().anyMatch(p -> sku.equals(p.getSku())), is(true));
	}

	@Test
	public void createInventoryProductV2ReturnsEntityAndProductCanBeRead() throws Exception {
		String sku = uniqueCode("v2-inventory-");
		PersistableCategory category = createPrivateCategory(uniqueCode("v2-inventory-cat-"));

		PersistableProduct product = super.product(sku);
		ArrayList<Category> categories = new ArrayList<>();
		categories.add(category);
		product.setCategories(categories);
		ProductSpecification specifications = new ProductSpecification();
		specifications.setManufacturer(
				com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer.DEFAULT_MANUFACTURER);
		product.setProductSpecifications(specifications);
		product.setPrice(BigDecimal.TEN);
		product.setSku(sku);

		ResponseEntity<Entity> createResponse = testRestTemplate.postForEntity(
				"/api/v2/private/product/inventory?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(product, getHeader()),
				Entity.class);
		assertThat(createResponse.getStatusCode(), is(CREATED));
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());

		ResponseEntity<ReadableProduct> getResponse = testRestTemplate.exchange(
				"/api/v2/product/" + sku + "?store=" + Constants.DEFAULT_STORE + "&lang=en",
				HttpMethod.GET,
				HttpEntity.EMPTY,
				ReadableProduct.class);
		assertThat(getResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(getResponse.getBody());
		assertThat(getResponse.getBody().getSku(), is(sku));
	}

	@Test
	public void optionAndOptionValueListsSupportNameFilterPaginationAndLocalization() throws Exception {
		String optionCode = uniqueCode("opt-filter-");
		PersistableProductOptionEntity option = new PersistableProductOptionEntity();
		option.setCode(optionCode);
		option.setType(ProductOptionType.Select.name());
		ProductOptionDescription optionDescription = new ProductOptionDescription();
		optionDescription.setLanguage("en");
		optionDescription.setName(optionCode);
		option.getDescriptions().add(optionDescription);

		ResponseEntity<ReadableProductOptionEntity> optionCreateResponse = testRestTemplate.postForEntity(
				"/api/v1/private/product/option?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(option, getHeader()),
				ReadableProductOptionEntity.class);
		assertThat(optionCreateResponse.getStatusCode(), is(CREATED));

		String optionValueCode = uniqueCode("opt-value-filter-");
		PersistableProductOptionValue optionValue = new PersistableProductOptionValue();
		optionValue.setCode(optionValueCode);
		ProductOptionValueDescription optionValueDescription = new ProductOptionValueDescription();
		optionValueDescription.setLanguage("en");
		optionValueDescription.setName(optionValueCode);
		optionValue.getDescriptions().add(optionValueDescription);

		ResponseEntity<ReadableProductOptionValue> optionValueCreateResponse = testRestTemplate.postForEntity(
				"/api/v1/private/product/option/value?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(optionValue, getHeader()),
				ReadableProductOptionValue.class);
		assertThat(optionValueCreateResponse.getStatusCode(), is(CREATED));

		ResponseEntity<ReadableProductOptionList> optionListResponse = testRestTemplate.exchange(
				"/api/v1/private/product/options?store=" + Constants.DEFAULT_STORE
						+ "&lang=en&name=" + optionCode + "&page=0&count=1",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionList.class);
		assertThat(optionListResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(optionListResponse.getBody());
		assertNotNull(optionListResponse.getBody().getOptions());
		assertThat(optionListResponse.getBody().getOptions().stream()
				.anyMatch(o -> optionCode.equals(o.getCode())), is(true));

		ResponseEntity<ReadableProductOptionValueList> optionValueListResponse = testRestTemplate.exchange(
				"/api/v1/private/product/options/values?store=" + Constants.DEFAULT_STORE
						+ "&lang=en&name=" + optionValueCode + "&page=0&count=1",
				HttpMethod.GET,
				new HttpEntity<>(getHeader()),
				ReadableProductOptionValueList.class);
		assertThat(optionValueListResponse.getStatusCode(), is(HttpStatus.OK));
		assertNotNull(optionValueListResponse.getBody());
		assertNotNull(optionValueListResponse.getBody().getOptionValues());
		assertThat(optionValueListResponse.getBody().getOptionValues().stream()
				.anyMatch(v -> optionValueCode.equals(v.getCode())), is(true));
	}

	private String uniqueCode(String prefix) {
		return prefix + System.nanoTime();
	}

	private PersistableCategory createPrivateCategory(String code) {
		PersistableCategory newCategory = category(code, code);
		ResponseEntity<PersistableCategory> categoryResponse = testRestTemplate.postForEntity(
				"/api/v1/private/category?store=" + Constants.DEFAULT_STORE,
				new HttpEntity<>(newCategory, getHeader()),
				PersistableCategory.class);
		assertThat(categoryResponse.getStatusCode(), is(CREATED));
		assertNotNull(categoryResponse.getBody());
		assertNotNull(categoryResponse.getBody().getId());
		return categoryResponse.getBody();
	}

	private PersistableProductDefinition v2Definition(String sku, PersistableCategory category, int sortOrder, String name) {
		PersistableProductDefinition product = new PersistableProductDefinition();
		product.setSku(sku);
		product.setSortOrder(sortOrder);
		product.setPrice(BigDecimal.TEN);
		product.setQuantity(10);
		product.setManufacturer(
				com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer.DEFAULT_MANUFACTURER);

		ArrayList<Category> categories = new ArrayList<>();
		categories.add(category);
		product.setCategories(categories);

		ProductDescription description = new ProductDescription();
		description.setLanguage("en");
		description.setName(name);
		description.setTitle(name);
		description.setDescription(name);
		description.setFriendlyUrl(name.toLowerCase().replace(' ', '-'));
		product.getDescriptions().add(description);

		return product;
	}

}
