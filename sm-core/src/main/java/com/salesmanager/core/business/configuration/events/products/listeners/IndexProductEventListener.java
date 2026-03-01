package com.salesmanager.core.business.configuration.events.products.listeners;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import com.salesmanager.core.business.configuration.events.products.DeleteProductAttributeEvent;
import com.salesmanager.core.business.configuration.events.products.DeleteProductEvent;
import com.salesmanager.core.business.configuration.events.products.DeleteProductImageEvent;
import com.salesmanager.core.business.configuration.events.products.DeleteProductVariantEvent;
import com.salesmanager.core.business.configuration.events.products.ProductEvent;
import com.salesmanager.core.business.configuration.events.products.SaveProductAttributeEvent;
import com.salesmanager.core.business.configuration.events.products.SaveProductEvent;
import com.salesmanager.core.business.configuration.events.products.SaveProductImageEvent;
import com.salesmanager.core.business.configuration.events.products.SaveProductVariantEvent;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.search.SearchService;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.attribute.ProductAttribute;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.catalog.product.variant.ProductVariant;
import com.salesmanager.core.model.merchant.MerchantStore;

/**
 * Index product in search module if it is configured to do so !
 * 
 * Should receive events that a product was created or updated or deleted
 * 
 * @author carlsamson
 *
 */
@Component
public class IndexProductEventListener implements ApplicationListener<ProductEvent> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexProductEventListener.class);

	private final SearchService searchService;
	private final ProductService productService;
	private final boolean noIndex;

	public IndexProductEventListener(
			SearchService searchService,
			ProductService productService,
			@Value("${search.noindex:false}") boolean noIndex) {
		this.searchService = searchService;
		this.productService = productService;
		this.noIndex = noIndex;
	}

	/**
	 * Listens to ProductEvent and ProductVariantEvent
	 */
	@Override
	public void onApplicationEvent(ProductEvent event) {
		
		
		if(!noIndex) {

			if (event instanceof SaveProductEvent productEvent) {
				saveProduct(productEvent);
			}
	
			if (event instanceof DeleteProductEvent productEvent) {
				deleteProduct(productEvent);
			}
	
			if (event instanceof SaveProductVariantEvent variantEvent) {
				saveProductVariant(variantEvent);
			}
	
			if (event instanceof DeleteProductVariantEvent variantEvent) {
				deleteProductVariant(variantEvent);
			}
			
			if (event instanceof SaveProductImageEvent imageEvent) {
				saveProductImage(imageEvent);
			}
	
			if (event instanceof DeleteProductImageEvent imageEvent) {
				deleteProductImage(imageEvent);
			}
			
			if (event instanceof SaveProductAttributeEvent attributeEvent) {
				saveProductAttribute(attributeEvent);
			}
	
			if (event instanceof DeleteProductAttributeEvent attributeEvent) {
				deleteProductAttribute(attributeEvent);
			}
			
			
		
		}

	}
	
	private Product productOfEvent(ProductEvent event) {
		
		Product product = event.getProduct();
		MerchantStore store = product.getMerchantStore();
		try {

			/**
			 * Refresh product
			 */

			Product fullProduct = productService.findOne(product.getId(), store);
			
			if(fullProduct != null) {
				product = fullProduct;
			} else {
				LOGGER.warn("Product {} could not be reloaded before indexing", product.getId());
			}
			
		return product;
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

	void saveProduct(SaveProductEvent event) {
		
		try {
			Product product = productOfEvent(event);
			MerchantStore store = product.getMerchantStore();
	
			searchService.index(store, product);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void deleteProduct(DeleteProductEvent event) {

		Product product = event.getProduct();
		MerchantStore store = product.getMerchantStore();

		try {
			searchService.deleteDocument(store, product);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}
	}

	void saveProductVariant(SaveProductVariantEvent event) {

		Product product = productOfEvent(event);

		MerchantStore store = product.getMerchantStore();

		ProductVariant variant = event.getVariant();// to be removed

		/**
		 * add new variant to be saved
		 **/

		List<ProductVariant> filteredVariants = product.getVariants().stream()
				.filter(i -> !Objects.equals(variant.getId(), i.getId())).collect(Collectors.toList());

		filteredVariants.add(variant);

		Set<ProductVariant> allVariants = new HashSet<>(filteredVariants);
		product.setVariants(allVariants);

		try {
			searchService.index(store, product);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}

	}

	void deleteProductVariant(DeleteProductVariantEvent event) {

		Product product = productOfEvent(event);

		MerchantStore store = product.getMerchantStore();

		ProductVariant variant = event.getVariant();// to be removed

		/**
		 * remove variant to be saved
		 **/

		List<ProductVariant> filteredVariants = product.getVariants().stream()
				.filter(i -> !Objects.equals(variant.getId(), i.getId())).collect(Collectors.toList());

		Set<ProductVariant> allVariants = new HashSet<>(filteredVariants);
		product.setVariants(allVariants);

		try {
			searchService.index(store, product);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}

	}
	

	void saveProductImage(SaveProductImageEvent event) {

		Product product = productOfEvent(event);

		MerchantStore store = product.getMerchantStore();


		ProductImage image = event.getProductImage();// to be removed

		/**
		 * add new image to be saved
		 **/

		List<ProductImage> filteredImages = product.getImages().stream()
				.filter(i -> !Objects.equals(image.getId(), i.getId())).collect(Collectors.toList());

		filteredImages.add(image);

		Set<ProductImage> allImages = new HashSet<>(filteredImages);
		product.setImages(allImages);

		try {
			searchService.index(store, product);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}

	}
	
	void deleteProductImage(DeleteProductImageEvent event) {
		
		// Product updates already trigger re-indexing, so image deletion is a no-op here.
	}
	
	void saveProductAttribute(SaveProductAttributeEvent event) {

		Product product = productOfEvent(event);

		MerchantStore store = product.getMerchantStore();

		ProductAttribute attribute = event.getProductAttribute();// to be removed

		/**
		 * add new attribute to be saved
		 **/

		List<ProductAttribute> filteredAttributes = product.getAttributes().stream()
				.filter(i -> !Objects.equals(attribute.getId(), i.getId())).collect(Collectors.toList());

		filteredAttributes.add(attribute);

		Set<ProductAttribute> allAttributes = new HashSet<>(filteredAttributes);
		product.setAttributes(allAttributes);

		try {
			searchService.index(store, product);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}

	}
	
	void deleteProductAttribute(DeleteProductAttributeEvent event) {

		Product product = productOfEvent(event);

		MerchantStore store = product.getMerchantStore();

		/**
		 * add new attribute to be saved
		 **/

		List<ProductAttribute> filteredAttributes = product.getAttributes().stream()
				.filter(i -> !Objects.equals(event.getProductAttribute().getId(), i.getId())).collect(Collectors.toList());

		Set<ProductAttribute> allAttributes = new HashSet<>(filteredAttributes);
		product.setAttributes(allAttributes);

		try {
			searchService.index(store, product);
		} catch (ServiceException e) {
			throw new RuntimeException(e);
		}

	}

	/**
	 * Get document by product id and document exist if event is Product delete
	 * document create document if event is Variant get document get variants
	 * replace variant
	 */

}
