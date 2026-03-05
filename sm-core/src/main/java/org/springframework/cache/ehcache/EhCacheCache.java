package org.springframework.cache.ehcache;

import java.util.concurrent.Callable;

import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import net.sf.ehcache.Element;

/**
 * MIGRATION NOTE: Spring Framework 6 removed the Ehcache 2 Cache adapter. This compatibility wrapper keeps the legacy Spring Cache contract backed by net.sf.ehcache.
 */
class EhCacheCache implements Cache {

	private final net.sf.ehcache.Cache nativeCache;

	EhCacheCache(net.sf.ehcache.Cache nativeCache) {
		this.nativeCache = nativeCache;
	}

	@Override
	public String getName() {
		return nativeCache.getName();
	}

	@Override
	public net.sf.ehcache.Cache getNativeCache() {
		return nativeCache;
	}

	@Override
	public ValueWrapper get(Object key) {
		Element element = nativeCache.get(key);
		return toValueWrapper(element);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Class<T> type) {
		Element element = nativeCache.get(key);
		if (element == null) {
			return null;
		}
		Object value = element.getObjectValue();
		if (type != null && value != null && !type.isInstance(value)) {
			throw new IllegalStateException("Cached value is not of required type " + type.getName());
		}
		return (T) value;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Object key, Callable<T> valueLoader) {
		Element element = nativeCache.get(key);
		if (element != null) {
			return (T) element.getObjectValue();
		}
		try {
			T value = valueLoader.call();
			put(key, value);
			return value;
		} catch (Exception exception) {
			throw new ValueRetrievalException(key, valueLoader, exception);
		}
	}

	@Override
	public void put(Object key, Object value) {
		nativeCache.put(new Element(key, value));
	}

	@Override
	public ValueWrapper putIfAbsent(Object key, Object value) {
		Element existingElement = nativeCache.putIfAbsent(new Element(key, value));
		return toValueWrapper(existingElement);
	}

	@Override
	public void evict(Object key) {
		nativeCache.remove(key);
	}

	@Override
	public boolean evictIfPresent(Object key) {
		return nativeCache.remove(key);
	}

	@Override
	public void clear() {
		nativeCache.removeAll();
	}

	@Override
	public boolean invalidate() {
		nativeCache.removeAll();
		return true;
	}

	private ValueWrapper toValueWrapper(Element element) {
		return element == null ? null : new SimpleValueWrapper(element.getObjectValue());
	}

}
