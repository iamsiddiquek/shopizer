package com.salesmanager.core.business.configuration;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// MIGRATION NOTE: The Boot 4 migration intentionally retains Ehcache 2 compatibility here to avoid cache-behavior drift while the broader cache stack remains unchanged.
@SuppressWarnings("deprecation")
public class CoreCacheConfiguration {

  @Bean(name = "springCacheManager", destroyMethod = "shutdown")
  public CacheManager springCacheManager() {
    // MIGRATION NOTE: Replaced the legacy Ehcache Spring XML with programmatic cache definitions while preserving cache names and eviction settings.
    net.sf.ehcache.config.Configuration configuration =
        new net.sf.ehcache.config.Configuration().name("com.shopizer.core.cache");
    configuration.defaultCache(defaultCacheConfiguration());
    configuration.addCache(cacheConfiguration("com.shopizer.OBJECT_CACHE", 10000, 1200, 1200,
        MemoryStoreEvictionPolicy.LFU));
    configuration.addCache(cacheConfiguration("store", 1000, 0, 0, MemoryStoreEvictionPolicy.LFU));
    configuration.addCache(cacheConfiguration("countriesMap", 500, 0, 0, MemoryStoreEvictionPolicy.LFU));
    configuration.addCache(cacheConfiguration("countrByCode", 300, 0, 0, MemoryStoreEvictionPolicy.LFU));
    configuration.addCache(cacheConfiguration("zoneByCode", 800, 0, 0, MemoryStoreEvictionPolicy.LFU));
    configuration.addCache(cacheConfiguration("languageByCode", 50, 0, 0, MemoryStoreEvictionPolicy.LFU));
    // MIGRATION NOTE: Boot 4 tests load multiple web application contexts in the same JVM, so reuse the existing Ehcache manager when the legacy cache name matches instead of failing on duplicate manager registration.
    return CacheManager.create(configuration);
  }

  @Bean(name = "serviceCacheManager")
  public EhCacheCacheManager serviceCacheManager(
      @Qualifier("springCacheManager") CacheManager springCacheManager) {
    EhCacheCacheManager cacheManager = new EhCacheCacheManager();
    cacheManager.setCacheManager(springCacheManager);
    return cacheManager;
  }

  @Bean(name = "serviceCache")
  public Cache serviceCache(@Qualifier("serviceCacheManager") EhCacheCacheManager serviceCacheManager) {
    return serviceCacheManager.getCache("com.shopizer.OBJECT_CACHE");
  }

  private CacheConfiguration defaultCacheConfiguration() {
    CacheConfiguration cacheConfiguration = new CacheConfiguration();
    cacheConfiguration.setMaxElementsInMemory(10000);
    cacheConfiguration.setEternal(false);
    cacheConfiguration.setTimeToIdleSeconds(120);
    cacheConfiguration.setTimeToLiveSeconds(120);
    cacheConfiguration.setOverflowToDisk(false);
    cacheConfiguration.setDiskSpoolBufferSizeMB(30);
    cacheConfiguration.setMaxElementsOnDisk(10000000);
    cacheConfiguration.setDiskPersistent(false);
    cacheConfiguration.setDiskExpiryThreadIntervalSeconds(120);
    cacheConfiguration.setMemoryStoreEvictionPolicyFromObject(MemoryStoreEvictionPolicy.LRU);
    return cacheConfiguration;
  }

  private CacheConfiguration cacheConfiguration(String name, int maxElementsInMemory,
      long timeToIdleSeconds, long timeToLiveSeconds, MemoryStoreEvictionPolicy evictionPolicy) {
    CacheConfiguration cacheConfiguration = new CacheConfiguration(name, maxElementsInMemory);
    cacheConfiguration.setEternal(false);
    cacheConfiguration.setOverflowToDisk(false);
    cacheConfiguration.setMemoryStoreEvictionPolicyFromObject(evictionPolicy);
    if (timeToIdleSeconds > 0) {
      cacheConfiguration.setTimeToIdleSeconds(timeToIdleSeconds);
    }
    if (timeToLiveSeconds > 0) {
      cacheConfiguration.setTimeToLiveSeconds(timeToLiveSeconds);
    }
    return cacheConfiguration;
  }
}
