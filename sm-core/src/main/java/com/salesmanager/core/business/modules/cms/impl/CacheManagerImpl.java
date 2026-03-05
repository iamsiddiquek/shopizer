package com.salesmanager.core.business.modules.cms.impl;

import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.security.Security;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CacheManagerImpl implements CacheManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheManagerImpl.class);

  //private static final String LOCATION_PROPERTIES = "location";

  protected String location = null;

  @SuppressWarnings("rawtypes")
  private TreeCache treeCache = null;

  @SuppressWarnings("unchecked")
  protected void init(String namedCache, String locationFolder) {


    try {

      this.location = locationFolder;
      // manager = new DefaultCacheManager(repositoryFileName);

      VendorCacheManager manager = VendorCacheManager.getInstance();

      if (manager == null) {
        LOGGER.error("CacheManager is null");
        return;
      }
      
/*      @SuppressWarnings("rawtypes")
      Cache c = manager.getManager().getCache(namedCache);
      
      if(c != null) {
    	  f = new TreeCacheFactory();
    	  treeCache = f.createTreeCache(c);
    	  //this.treeCache = (TreeCache)c;
    	  return;
      }*/
      
      
      Configuration config = new ConfigurationBuilder()
    		   .persistence().passivation(false)
    		   .addSingleFileStore()
    		   .segmented(false)
    		   .location(location).async().enable()
    		   .preload(false).shared(false)
    		   .invocationBatching().enable()
    		   .build();
      
      // MIGRATION NOTE: Wrap named-cache bootstrap in Infinispan's thread-local Subject context
      // so Java 25 can execute legacy listener registration without changing cache semantics.
      Security.doAs(new Subject(), (PrivilegedAction<Void>) () -> {
        manager.getManager().defineConfiguration(namedCache, config);

        final Cache<String, String> cache = manager.getManager().getCache(namedCache);

        TreeCacheFactory treeCacheFactory = new TreeCacheFactory();
        treeCache = treeCacheFactory.createTreeCache(cache);
        cache.start();
        return null;
      });

      LOGGER.debug("CMS started");



    } catch (Exception e) {
      LOGGER.error("Error while instantiating CmsImageFileManager", e);
    } finally {

    }



  }

  public EmbeddedCacheManager getManager() {
    return VendorCacheManager.getInstance().getManager();
  }

  @SuppressWarnings("rawtypes")
  public TreeCache getTreeCache() {
    return treeCache;
  }



}
