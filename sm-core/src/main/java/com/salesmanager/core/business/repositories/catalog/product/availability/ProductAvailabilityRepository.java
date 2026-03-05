package com.salesmanager.core.business.repositories.catalog.product.availability;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.salesmanager.core.model.catalog.product.availability.ProductAvailability;

// MIGRATION NOTE: Suppress the inherited getById deprecation warning because this repository intentionally keeps its legacy method name and custom fetch-join semantics unchanged.
@SuppressWarnings("deprecation")
public interface ProductAvailabilityRepository extends JpaRepository<ProductAvailability, Long> {

  
  // MIGRATION NOTE: Hibernate 6 rejects duplicate fetch joins for the same association alias; removing the redundant merchantStore fetch preserves the original result graph.
  @Query(value = "select distinct p from ProductAvailability p "
      + "left join fetch p.merchantStore pm "
      + "left join fetch p.prices pp "
      + "left join fetch pp.descriptions ppd "
      + "join fetch p.product ppr "
      + "join fetch ppr.merchantStore pprm "
      + "where p.id=?1 ")
  ProductAvailability getById(Long availabilityId);
  
  // MIGRATION NOTE: Hibernate 6 rejects duplicate fetch joins for the same association alias; removing the redundant merchantStore fetch preserves the original result graph.
  @Query(value = "select distinct p from ProductAvailability p "
      + "left join fetch p.merchantStore pm "
      + "left join fetch p.prices pp "
      + "left join fetch pp.descriptions ppd "
      + "join fetch p.product ppr "
      + "join fetch ppr.merchantStore pprm "
      + "where p.id=?1 "
      + "and pprm.id=?2")
  ProductAvailability getById(Long availabilityId, int merchantId);
  

  @Query(value = "select distinct p from ProductAvailability p "
	      + "left join fetch p.merchantStore pm "
	      + "left join fetch p.prices pp "
	      + "left join fetch pp.descriptions ppd "
	      + "join fetch p.product ppr "
	      + "left join fetch ppr.descriptions pprd "
	      + "left join fetch p.productVariant ppi "
	      + "where ppr.sku=?1 or ppi.sku=?1 "
	      + "and pm.code=?2")
  List<ProductAvailability> getBySku(String productCode, String store);
  
  @Query(value = "select distinct p from ProductAvailability p "
	      + "left join fetch p.merchantStore pm "
	      + "left join fetch p.prices pp "
	      + "left join fetch pp.descriptions ppd "
	      + "join fetch p.product ppr "
	      + "left join fetch ppr.descriptions pprd "
	      + "left join fetch p.productVariant ppi "
	      + "where ppr.sku=?1 or ppi.sku=?1")
  List<ProductAvailability> getBySku(String sku);

}
