package com.salesmanager.shop.utils;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import com.salesmanager.core.model.common.Criteria;
import com.salesmanager.core.model.common.CriteriaOrderBy;
import com.salesmanager.core.model.merchant.MerchantStoreCriteria;
import com.salesmanager.shop.store.api.exception.RestApiException;

public final class ServiceRequestCriteriaBuilderUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRequestCriteriaBuilderUtils.class);

	private ServiceRequestCriteriaBuilderUtils() {
	}
	
	/**
	 * Binds request parameter values to specific request criterias
	 * @param criteria
	 * @param mappingFields
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public static Criteria buildRequestCriterias(Criteria criteria, Map<String, String> mappingFields, HttpServletRequest request) throws RestApiException {
		
			if(criteria == null)
				throw new RestApiException("A criteria class type must be instantiated");

			if (mappingFields == null || mappingFields.isEmpty()) {
				return criteria;
			}

			mappingFields.forEach((parameterName, fieldName) -> {
				try {
					setValue(criteria, request, parameterName, fieldName);
				} catch (Exception e) {
					LOGGER.warn("Skipping invalid criteria binding for request parameter '{}'", parameterName, e);
				}
			});
			return criteria;
		

		
	}
	
	private static void setValue(Criteria criteria, HttpServletRequest request, String parameterName, String setterValue) throws Exception {
		
		
		try {
			
			PropertyAccessor criteriaAccessor = PropertyAccessorFactory.forDirectFieldAccess(criteria);
			
			
			String parameterValue = request.getParameter(parameterName);
			if(parameterValue == null) return;
			// set the property directly, bypassing the mutator (if any)
			String setterName = setterValue;
			LOGGER.debug("Binding request parameter '{}' to '{}'", parameterName, setterName);
			criteriaAccessor.setPropertyValue(setterName, parameterValue);
		
		} catch(Exception e) {
			throw new Exception("An error occurred while binding request parameters", e);
		}
		
		
	}
		   
  /** deprecated **/
  public static Criteria buildRequest(Map<String, String> mappingFields, HttpServletRequest request) {
    
    /**
     * Works assuming datatable sends query data
     */
    MerchantStoreCriteria criteria = new MerchantStoreCriteria();

    String searchParam = request.getParameter("search[value]");
    String orderColums = request.getParameter("order[0][column]");

    if (!StringUtils.isBlank(orderColums)) {
      String columnName = request.getParameter("columns[" + orderColums + "][data]");
      String overwriteField = columnName;
      if (mappingFields != null && mappingFields.get(columnName) != null) {
        overwriteField = mappingFields.get(columnName);
      }
      criteria.setCriteriaOrderByField(overwriteField);
      criteria.setOrderBy(
          CriteriaOrderBy.valueOf(request.getParameter("order[0][dir]").toUpperCase()));
    }
    
    String storeName = request.getParameter("storeName");
    criteria.setName(storeName);
    
    String retailers = request.getParameter("retailers");
    String stores = request.getParameter("stores");
    
    try {
    	boolean retail = Boolean.valueOf(retailers);
    	boolean sto = Boolean.valueOf(stores);

        criteria.setRetailers(retail);
        criteria.setStores(sto);
    } catch(Exception e) {
    	LOGGER.error("Error parsing boolean values",e);
    }
    
    criteria.setSearch(searchParam);

    return criteria;
    
  }

}
