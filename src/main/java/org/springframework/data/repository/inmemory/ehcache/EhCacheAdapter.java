/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.repository.inmemory.ehcache;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.SearchAttribute;
import net.sf.ehcache.config.Searchable;

import org.springframework.beans.BeanUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.repository.inmemory.InMemoryAdapter;
import org.springframework.data.util.ListConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * {@link InMemoryAdapter} implementation using {@link CacheManager}.
 * 
 * @author Christoph Strobl
 */
public class EhCacheAdapter implements InMemoryAdapter {

	private CacheManager cacheManager;

	public EhCacheAdapter() {
		this(CacheManager.create());
	}

	public EhCacheAdapter(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#put(java.io.Serializable, java.lang.Object)
	 */
	@Override
	public Object put(Serializable id, Object item) {

		Element element = new Element(id, item);
		getCache(item).put(element);
		return item;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#contains(java.io.Serializable, java.lang.Class)
	 */
	@Override
	public boolean contains(Serializable id, Class<?> type) {
		return get(id, type) != null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#get(java.io.Serializable, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(Serializable id, Class<T> type) {
		Element element = getCache(type).get(id);
		return (T) (element != null ? element.getObjectValue() : null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#delete(java.io.Serializable, java.lang.Class)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> T delete(Serializable id, Class<T> type) {

		Element element = getCache(type).removeAndReturnElement(id);
		return (T) (element != null ? element.getObjectValue() : null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#getAllOf(java.lang.Class)
	 */
	@Override
	public <T> Collection<T> getAllOf(Class<T> type) {

		Collection<Element> values = getCache(type).getAll(getCache(type).getKeys()).values();
		return new ListConverter<Element, T>(new ElementConverter<T>()).convert(values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#deleteAllOf(java.lang.Class)
	 */
	@Override
	public void deleteAllOf(Class<?> type) {
		getCache(type).removeAll();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.inmemory.InMemoryAdapter#clear()
	 */
	@Override
	public void clear() {
		cacheManager.clearAll();
	}

	protected Cache getCache(Object item) {

		Assert.notNull(item, "Item must not be 'null' for lookup.");
		return getCache(item.getClass());
	}

	protected Cache getCache(final Class<?> type) {

		Assert.notNull(type, "Type must not be 'null' for lookup.");
		Class<?> userType = ClassUtils.getUserClass(type);

		if (!cacheManager.cacheExists(userType.getName())) {

			CacheConfiguration cacheConfig = cacheManager.getConfiguration().getDefaultCacheConfiguration().clone();

			if (!cacheConfig.isSearchable()) {

				cacheConfig = new CacheConfiguration();
				cacheConfig.setMaxEntriesLocalHeap(0);
			}
			cacheConfig.setName(userType.getName());
			final Searchable s = new Searchable();

			// TODO: maybe use mappingcontex information at this point or register generic type using some spel expression
			// validator
			ReflectionUtils.doWithFields(userType, new FieldCallback() {

				@Override
				public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {

					PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(type, field.getName());

					if (pd != null && pd.getReadMethod() != null) {
						s.addSearchAttribute(new SearchAttribute().name(field.getName()).expression(
								"value." + pd.getReadMethod().getName() + "()"));
					}
				}
			});

			cacheConfig.addSearchable(s);
			cacheManager.addCache(new Cache(cacheConfig));
		}
		return cacheManager.getCache(userType.getName());
	}

	private class ElementConverter<T> implements Converter<Element, T> {

		@SuppressWarnings("unchecked")
		@Override
		public T convert(Element source) {
			return (T) source.getObjectValue();
		}
	}
}
