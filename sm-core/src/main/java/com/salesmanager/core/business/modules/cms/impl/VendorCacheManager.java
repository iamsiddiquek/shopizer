package com.salesmanager.core.business.modules.cms.impl;

import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.security.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VendorCacheManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(VendorCacheManager.class);
  private EmbeddedCacheManager manager = null;
  private static VendorCacheManager vendorCacheManager = null;


  private VendorCacheManager() {

    try {
      // MIGRATION NOTE: Wrap Infinispan startup in its own thread-local Subject to preserve legacy
      // cache bootstrap behavior on Java 25, where Subject.getSubject(AccessControlContext) is no
      // longer supported during DefaultCacheManager initialization.
      manager = Security.doAs(new Subject(),
          (PrivilegedAction<EmbeddedCacheManager>) DefaultCacheManager::new);
    } catch (Exception e) {
      LOGGER.error("Cannot start manager {}", e.toString(), e);
    }

  }


  public static VendorCacheManager getInstance() {
    if (vendorCacheManager == null) {
      vendorCacheManager = new VendorCacheManager();

    }
    return vendorCacheManager;
  }


  public EmbeddedCacheManager getManager() {
    return manager;
  }

}
