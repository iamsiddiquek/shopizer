package org.springframework.cache.ehcache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

import net.sf.ehcache.CacheManager;

/**
 * MIGRATION NOTE: Spring Framework 6 removed the Ehcache 2 CacheManager adapter. This compatibility manager preserves the legacy XML bean contract and cache names during the phased migration.
 */
public class EhCacheCacheManager extends AbstractCacheManager {

	private CacheManager cacheManager;

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	@Override
	protected Collection<? extends Cache> loadCaches() {
		List<Cache> caches = new ArrayList<>();
		if (cacheManager == null) {
			return caches;
		}
		for (String cacheName : cacheManager.getCacheNames()) {
			net.sf.ehcache.Cache nativeCache = cacheManager.getCache(cacheName);
			if (nativeCache != null) {
				caches.add(new EhCacheCache(nativeCache));
			}
		}
		return caches;
	}

	@Override
	protected Cache getMissingCache(String name) {
		if (cacheManager == null) {
			return null;
		}
		net.sf.ehcache.Cache nativeCache = cacheManager.getCache(name);
		return nativeCache == null ? null : new EhCacheCache(nativeCache);
	}

}
