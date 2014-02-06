/*
 * Copyright 2012-2014 the original author or authors.
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
package org.springframework.data.repository.support;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.core.CrudInvoker;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.support.RepositoryFactoryInformation;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Wrapper class to access repository instances obtained from a {@link ListableBeanFactory}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class Repositories implements Iterable<Class<?>> {

	static final Repositories NONE = new Repositories();
	static final RepositoryFactoryInformation<Object, Serializable> EMPTY_REPOSITORY_FACTORY_INFO = new EmptyRepositoryFactoryInformation();

	private final BeanFactory beanFactory;
	private final Map<Class<?>, String> repositoryBeanNames;
	private final Map<Class<?>, RepositoryFactoryInformation<Object, Serializable>> repositoryFactoryInfos;

	/**
	 * Constructor to create the {@link #NONE} instance.
	 */
	private Repositories() {

		this.beanFactory = null;
		this.repositoryBeanNames = Collections.<Class<?>, String> emptyMap();
		this.repositoryFactoryInfos = Collections.<Class<?>, RepositoryFactoryInformation<Object, Serializable>> emptyMap();
	}

	/**
	 * Creates a new {@link Repositories} instance by looking up the repository instances and meta information from the
	 * given {@link ListableBeanFactory}.
	 * 
	 * @param factory must not be {@literal null}.
	 */
	public Repositories(ListableBeanFactory factory) {

		Assert.notNull(factory);

		this.beanFactory = factory;
		this.repositoryFactoryInfos = new HashMap<Class<?>, RepositoryFactoryInformation<Object, Serializable>>();
		this.repositoryBeanNames = new HashMap<Class<?>, String>();

		populateRepositoryFactoryInformation(factory);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void populateRepositoryFactoryInformation(ListableBeanFactory factory) {

		Set<Map.Entry<String, RepositoryFactoryInformation>> repositoryFactoryBeans = BeanFactoryUtils
				.beansOfTypeIncludingAncestors(factory, RepositoryFactoryInformation.class).entrySet();

		for (Map.Entry<String, RepositoryFactoryInformation> entry : repositoryFactoryBeans) {

			String beanName = entry.getKey();
			// if (BeanFactoryUtils.isFactoryDereference(beanName)) {

			RepositoryFactoryInformation repositoryFactoryInformation = entry.getValue();
			Class<?> userDomainType = ClassUtils.getUserClass(repositoryFactoryInformation.getRepositoryInformation()
					.getDomainType());

			this.repositoryFactoryInfos.put(userDomainType, repositoryFactoryInformation);
			this.repositoryBeanNames.put(userDomainType, BeanFactoryUtils.transformedBeanName(beanName));
			// }
		}
	}

	/**
	 * Returns whether we have a repository instance registered to manage instances of the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public boolean hasRepositoryFor(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		return repositoryFactoryInfos.containsKey(domainClass);
	}

	/**
	 * Returns the repository managing the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public Object getRepositoryFor(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		String repositoryBeanName = repositoryBeanNames.get(domainClass);
		return repositoryBeanName == null || beanFactory == null ? null : beanFactory.getBean(repositoryBeanName);
	}

	/**
	 * Returns the {@link RepositoryFactoryInformation} for the given domain class. The given <code>code</code> is
	 * converted to the actual user class if necessary, @see ClassUtils#getUserClass.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryFactoryInformation} for the given domain class or {@literal null} if no repository
	 *         registered for this domain class.
	 */
	private RepositoryFactoryInformation<Object, Serializable> getRepositoryFactoryInfoFor(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		RepositoryFactoryInformation<Object, Serializable> repositoryInfo = repositoryFactoryInfos.get(ClassUtils
				.getUserClass(domainClass));
		return repositoryInfo == null ? EMPTY_REPOSITORY_FACTORY_INFO : repositoryInfo;
	}

	/**
	 * Returns the {@link EntityInformation} for the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T, S extends Serializable> EntityInformation<T, S> getEntityInformationFor(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		return (EntityInformation<T, S>) getRepositoryFactoryInfoFor(domainClass).getEntityInformation();
	}

	/**
	 * Returns the {@link RepositoryInformation} for the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link RepositoryInformation} for the given domain class or {@literal null} if no repository registered
	 *         for this domain class.
	 */
	public RepositoryInformation getRepositoryInformationFor(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		RepositoryFactoryInformation<Object, Serializable> information = getRepositoryFactoryInfoFor(domainClass);
		return information == EMPTY_REPOSITORY_FACTORY_INFO ? null : information.getRepositoryInformation();
	}

	/**
	 * Returns the {@link PersistentEntity} for the given domain class. Might return {@literal null} in case the module
	 * storing the given domain class does not support the mapping subsystem.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return the {@link PersistentEntity} for the given domain class or {@literal null} if no repository is registered
	 *         for the domain class or the repository is not backed by a {@link MappingContext} implementation.
	 */
	public PersistentEntity<?, ?> getPersistentEntity(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		return getRepositoryFactoryInfoFor(domainClass).getPersistentEntity();
	}

	/**
	 * Returns the {@link QueryMethod}s contained in the repository managing the given domain class.
	 * 
	 * @param domainClass must not be {@literal null}.
	 * @return
	 */
	public List<QueryMethod> getQueryMethodsFor(Class<?> domainClass) {

		Assert.notNull(domainClass, "Domain class must not be null!");

		return getRepositoryFactoryInfoFor(domainClass).getQueryMethods();
	}

	@SuppressWarnings("unchecked")
	public <T> CrudInvoker<T> getCrudInvoker(Class<T> domainClass) {

		Object repository = getRepositoryFor(domainClass);

		Assert.notNull(repository, String.format("No repository found for domain class: %s", domainClass));

		if (repository instanceof CrudRepository) {
			return new CrudRepositoryInvoker<T>((CrudRepository<T, Serializable>) repository);
		} else {
			return new ReflectionRepositoryInvoker<T>(repository, getRepositoryInformationFor(domainClass).getCrudMethods());
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<Class<?>> iterator() {
		return repositoryFactoryInfos.keySet().iterator();
	}

	static class EmptyRepositoryFactoryInformation implements RepositoryFactoryInformation<Object, Serializable> {

		@Override
		public EntityInformation<Object, Serializable> getEntityInformation() {
			return null;
		}

		@Override
		public RepositoryInformation getRepositoryInformation() {
			return null;
		}

		@Override
		public PersistentEntity<?, ?> getPersistentEntity() {
			return null;
		}

		@Override
		public List<QueryMethod> getQueryMethods() {
			return Collections.<QueryMethod> emptyList();
		}
	}
}
