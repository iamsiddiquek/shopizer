package com.salesmanager.shop.populator.store;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.currency.CurrencyService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.utils.AbstractDataPopulator;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.currency.Currency;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;
import com.salesmanager.shop.model.references.PersistableAddress;
import com.salesmanager.shop.model.store.PersistableMerchantStore;
import com.salesmanager.shop.utils.DateUtil;

@Component
public class PersistableMerchantStorePopulator extends AbstractDataPopulator<PersistableMerchantStore, MerchantStore> {

	private final CountryService countryService;
	private final ZoneService zoneService;
	private final LanguageService languageService;
	private final CurrencyService currencyService;
	private final MerchantStoreService merchantStoreService;

	public PersistableMerchantStorePopulator(
			CountryService countryService,
			ZoneService zoneService,
			LanguageService languageService,
			CurrencyService currencyService,
			MerchantStoreService merchantStoreService) {
		this.countryService = countryService;
		this.zoneService = zoneService;
		this.languageService = languageService;
		this.currencyService = currencyService;
		this.merchantStoreService = merchantStoreService;
	}
	
	
	@Override
	public MerchantStore populate(PersistableMerchantStore source, MerchantStore target, MerchantStore store,
			Language language) throws ConversionException {

		Validate.notNull(source, "PersistableMerchantStore mst not be null");
		
		if(target == null) {
			target = new MerchantStore();
		}
		
		target.setCode(source.getCode());
		if(source.getId()!=0) {
			target.setId(source.getId());
		}
		
		if(store.getStoreLogo()!=null) {
			target.setStoreLogo(store.getStoreLogo());
		}
		
		if(!StringUtils.isEmpty(source.getInBusinessSince())) {
			try {
				Date dt = DateUtil.getDate(source.getInBusinessSince());
				target.setInBusinessSince(dt);
			} catch(Exception e) {
				throw new ConversionException("Cannot parse date [" + source.getInBusinessSince() + "]",e);
			}
		}

		if(source.getDimension()!=null) {
		  target.setSeizeunitcode(source.getDimension().name());
		}
		if(source.getWeight()!=null) {
		  target.setWeightunitcode(source.getWeight().name());
		}
		target.setCurrencyFormatNational(source.isCurrencyFormatNational());
		target.setStorename(source.getName());
		target.setStorephone(source.getPhone());
		target.setStoreEmailAddress(source.getEmail());
		target.setUseCache(source.isUseCache());
		target.setRetailer(source.isRetailer());
		
		//get parent store
		if(!StringUtils.isBlank(source.getRetailerStore())) {
		  if(source.getRetailerStore().equals(source.getCode())) {
		    throw new ConversionException("Parent store [" + source.getRetailerStore() + "] cannot be parent of current store");
		  }
		  try {
            MerchantStore parent = merchantStoreService.getByCode(source.getRetailerStore());
            if(parent == null) {
              throw new ConversionException("Parent store [" + source.getRetailerStore() + "] does not exist");
            }
            target.setParent(parent);
          } catch (ServiceException e) {
              throw new ConversionException(e);
          }
		}
		
		
			try {
				
				String defaultLanguageCode = !StringUtils.isEmpty(source.getDefaultLanguage())
						? source.getDefaultLanguage()
						: languageService.defaultLanguage().getCode();
				Language l = buildLanguageReference(defaultLanguageCode);
				target.setDefaultLanguage(l);
				
				if(!StringUtils.isEmpty(source.getCurrency())) {
					Currency c = buildCurrencyReference(source.getCurrency());
					target.setCurrency(c);
				} else {
					target.setCurrency(buildCurrencyReference(Constants.DEFAULT_CURRENCY.getCurrencyCode()));
				}
				
				List<String> languages = source.getSupportedLanguages();
				if(!CollectionUtils.isEmpty(languages)) {
					Set<String> addedLanguageCodes = new LinkedHashSet<String>();
					for(String lang : languages) {
						if (StringUtils.isBlank(lang)) {
							continue;
						}
						Language ll = buildLanguageReference(lang);
						if (addedLanguageCodes.add(ll.getCode())) {
							target.getLanguages().add(ll);
						}
					}
				}
			
		} catch(Exception e) {
			throw new ConversionException(e);
		}
		
		//address population
		PersistableAddress address = source.getAddress();
		if(address != null) {
			Country country = buildCountryReference(address.getCountry());

			Zone zone = zoneService.getByCode(address.getStateProvince());
			if(zone != null) {
				target.setZone(zone);
			} else {
				target.setStorestateprovince(address.getStateProvince());
			}
			
			target.setStoreaddress(address.getAddress());
			target.setStorecity(address.getCity());
			target.setCountry(country);
			target.setStorepostalcode(address.getPostalCode());
		}

		if (StringUtils.isNotEmpty(source.getTemplate()))
			target.setStoreTemplate(source.getTemplate());
		
		return target;
	}

	private Language buildLanguageReference(String code) throws ConversionException {
		Language dependency = new Language();
		dependency.setCode(normalizeLanguageCode(code));
		dependency.setSortOrder(0);
		return dependency;
	}

	private Currency buildCurrencyReference(String code) throws ConversionException {
		String normalized = normalizeCode(code, "Currency");
		try {
			java.util.Currency javaCurrency = java.util.Currency.getInstance(normalized);
			Currency dependency = new Currency();
			dependency.setCurrency(javaCurrency);
			dependency.setName(javaCurrency.getCurrencyCode());
			dependency.setSupported(true);
			return dependency;
		} catch (IllegalArgumentException e) {
			throw new ConversionException("Cannot create currency [" + normalized + "]", e);
		}
	}

	private Country buildCountryReference(String code) throws ConversionException {
		Country dependency = new Country();
		dependency.setIsoCode(normalizeCode(code, "Country"));
		dependency.setSupported(true);
		return dependency;
	}

	private String normalizeCode(String code, String dependencyName) throws ConversionException {
		if (StringUtils.isBlank(code)) {
			throw new ConversionException(dependencyName + " code is required");
		}
		return code.trim().toUpperCase(Locale.ROOT);
	}

	private String normalizeLanguageCode(String code) throws ConversionException {
		if (StringUtils.isBlank(code)) {
			throw new ConversionException("Language code is required");
		}
		return code.trim().toLowerCase(Locale.ROOT);
	}

	@Override
	protected MerchantStore createTarget() {
		// TODO Auto-generated method stub
		return null;
	}

}
