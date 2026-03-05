package org.springframework.cache.ehcache;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

/**
 * MIGRATION NOTE: Spring Framework 6 removed the Ehcache 2 bridge. This compatibility factory preserves the legacy XML bean contract during the phased migration.
 */
public class EhCacheManagerFactoryBean implements FactoryBean<CacheManager>, InitializingBean, DisposableBean {

	private Resource configLocation;
	private boolean shared;
	private CacheManager cacheManager;

	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	public void setShared(boolean shared) {
		this.shared = shared;
	}

	@Override
	public void afterPropertiesSet() throws IOException {
		if (configLocation != null) {
			try (InputStream inputStream = configLocation.getInputStream()) {
				Configuration configuration = ConfigurationFactory.parseConfiguration(inputStream);
				this.cacheManager = shared ? CacheManager.create(configuration) : CacheManager.newInstance(configuration);
			}
			return;
		}
		this.cacheManager = shared ? CacheManager.create() : CacheManager.newInstance();
	}

	@Override
	public CacheManager getObject() {
		return cacheManager;
	}

	@Override
	public Class<?> getObjectType() {
		return CacheManager.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void destroy() {
		if (cacheManager != null) {
			cacheManager.shutdown();
		}
	}

}
