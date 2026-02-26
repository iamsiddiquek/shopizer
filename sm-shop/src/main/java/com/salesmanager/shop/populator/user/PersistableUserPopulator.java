package com.salesmanager.shop.populator.user;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.salesmanager.core.business.exception.ConversionException;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.utils.AbstractDataPopulator;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.user.Group;
import com.salesmanager.core.model.user.GroupType;
import com.salesmanager.core.model.user.User;
import com.salesmanager.shop.model.security.PersistableGroup;
import com.salesmanager.shop.model.user.PersistableUser;


@Component
public class PersistableUserPopulator extends AbstractDataPopulator<PersistableUser, User> {

  @Inject
  private LanguageService languageService;
  
  @Inject
  private MerchantStoreService merchantStoreService;
  
  @Inject
  @Named("passwordEncoder")
  private PasswordEncoder passwordEncoder;
  
  @Override
  public User populate(PersistableUser source, User target, MerchantStore store, Language language)
      throws ConversionException {
    Validate.notNull(source, "PersistableUser cannot be null");
    Validate.notNull(store, "MerchantStore cannot be null");

    if (target == null) {
      target = new User();
    }

    target.setFirstName(source.getFirstName());
    target.setLastName(source.getLastName());
    target.setAdminEmail(source.getEmailAddress());
    target.setAdminName(source.getUserName());
    if(!StringUtils.isBlank(source.getPassword())) {
      target.setAdminPassword(passwordEncoder.encode(source.getPassword()));
    }
    
    if(!StringUtils.isBlank(source.getStore())) {
        try {
			MerchantStore userStore = merchantStoreService.getByCode(source.getStore());
			if (userStore == null) {
				throw new ConversionException("MerchantStore store [" + source.getStore() + "] does not exist");
			}
			target.setMerchantStore(userStore);
		} catch (ServiceException e) {
			throw new ConversionException("Error while reading MerchantStore store [" + source.getStore() + "]",e);
		}
    } else {
    	target.setMerchantStore(store);
    }
    
    
    target.setActive(source.isActive());
    
    Language lang = buildLanguageReference(source.getDefaultLanguage());

    // set default language
    target.setDefaultLanguage(lang);

    List<Group> userGroups = buildGroupReferences(source.getGroups());
    
    target.setGroups(userGroups);

    return target;
  }

  private Language buildLanguageReference(String languageCode) {
    String code = StringUtils.isBlank(languageCode)
        ? normalizeLanguageCode(languageService.defaultLanguage().getCode())
        : normalizeLanguageCode(languageCode);
    Language dependency = new Language();
    dependency.setCode(code);
    dependency.setSortOrder(0);
    return dependency;
  }

  private List<Group> buildGroupReferences(List<PersistableGroup> sourceGroups) {
    List<Group> userGroups = new ArrayList<Group>();
    if (sourceGroups == null) {
      return userGroups;
    }

    Set<String> processedNames = new LinkedHashSet<String>();
    for (PersistableGroup sourceGroup : sourceGroups) {
      if (sourceGroup == null || StringUtils.isBlank(sourceGroup.getName())) {
        continue;
      }

      String groupName = sourceGroup.getName().trim().toUpperCase(Locale.ROOT);
      if (!processedNames.add(groupName)) {
        continue;
      }

      userGroups.add(buildGroupReference(sourceGroup, groupName));
    }

    return userGroups;
  }

  private Group buildGroupReference(PersistableGroup sourceGroup, String groupName) {
    Group dependency = new Group();
    dependency.setGroupName(groupName);
    dependency.setGroupType(resolveGroupType(sourceGroup));
    return dependency;
  }

  private GroupType resolveGroupType(PersistableGroup sourceGroup) {
    if (sourceGroup != null && !StringUtils.isBlank(sourceGroup.getType())) {
      try {
        return GroupType.valueOf(sourceGroup.getType().trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ignored) {
        // Fallback to inferred type below.
      }
    }

    return "CUSTOMER".equalsIgnoreCase(sourceGroup != null ? sourceGroup.getName() : null)
        ? GroupType.CUSTOMER
        : GroupType.ADMIN;
  }

  private String normalizeLanguageCode(String code) {
    return code.trim().toLowerCase(Locale.ROOT);
  }

  @Override
  protected User createTarget() {
    // TODO Auto-generated method stub
    return null;
  }

}
