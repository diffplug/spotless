/*
 * Copyright 2016-2021 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.spotless.extra.eclipse.base.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.osgi.internal.framework.DTOBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.ServiceReferenceDTO;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseServiceConfig;

/**
 * Collection of services.
 * Eclipse service are not expected to hold any resources. Spotless services
 * can implement AutoCloseable in case a resource release is required on shutdown.
 *
 * Note that the collection access is not thread save, since it is expected
 * that the collection is completed before starting any bundles.
 */
public class ServiceCollection implements SpotlessEclipseServiceConfig {
	private final Map<String, ServiceReference<?>> className2Service;
	private final List<AutoCloseable> servicesWithResources;
	private final Bundle systemBundle;
	private final Map<String, String> properties;

	/**
	 * Collection of services
	 * @param systemBundle All services will belong to the system bundles
	 * @param All services share the same properties
	 */
	ServiceCollection(Bundle systemBundle, Map<String, String> properties) {
		className2Service = new HashMap<String, ServiceReference<?>>();
		servicesWithResources = new ArrayList<>();
		this.systemBundle = systemBundle;
		this.properties = properties;
	}

	void stop() {
		servicesWithResources.stream().forEach(s -> {
			try {
				s.close();
			} catch (Exception e) {
				//Stop on best effort basis
			}
		});
	}

	@Override
	public void set(String key, String value) {
		properties.put(key, value);
	}

	@Override
	public <S> void add(Class<S> interfaceClass, S service) throws ServiceException {
		String className = interfaceClass.getName();
		if (null != className2Service.put(interfaceClass.getName(), new FrameworkServiceReference<S>(className, service))) {
			throw new ServiceException(
					String.format("Service '%s' is already registered.", interfaceClass.getName()), ServiceException.FACTORY_ERROR);
		}
		if (service instanceof AutoCloseable) {
			servicesWithResources.add((AutoCloseable) service);
		}
	}

	/** Creates filter object suitable to lookup service by its interface name. */
	Filter createFilter(String filterDescr) {
		Optional<String> serviceClassName = className2Service.keySet().stream().filter(serviceClazzName -> filterDescr.contains(serviceClazzName)).findFirst();
		return new ClassNameBasedFilter(serviceClassName);
	}

	/**
	 * Get reference matching interface class name or all references
	 * @param interfaceClassName Class name filter. All references are returned in case filter is null
	 * @return Matching or all interfaces. If no interface is matching the filter, null is returned.
	 */
	ServiceReference<?>[] getReferences(String interfaceClassName) {
		if (null == interfaceClassName) {
			Collection<ServiceReference<?>> allServices = className2Service.values();
			return allServices.toArray(new ServiceReference<?>[allServices.size()]);
		}
		ServiceReference<?> singleService = className2Service.get(interfaceClassName);
		return (null == singleService) ? null : new ServiceReference<?>[]{singleService};
	}

	/**
	 * Return service for reference if it belongs to the system bundle.
	 * @param reference Service reference
	 * @return null, if service does not belong to the system bundle
	 */
	<S> S getService(ServiceReference<S> reference) {
		if (systemBundle == reference.getBundle()) {
			return ((FrameworkServiceReference<S>) reference).getService();
		}
		return null;
	}

	/** References to static services (not modifiable at run-time */
	private class FrameworkServiceReference<S> implements ServiceReference<S> {

		private final String className;
		private final S service;

		private FrameworkServiceReference(String className, S service) {
			this.className = className;
			this.service = service;
		}

		private S getService() {
			return service;
		}

		@Override
		public java.lang.Object getProperty(String key) {
			return properties.get(key);
		}

		@Override
		public String[] getPropertyKeys() {
			return properties.keySet().toArray(new String[properties.size()]);
		}

		@Override
		public Bundle getBundle() {
			return systemBundle;
		}

		@Override
		public Bundle[] getUsingBundles() {
			return new Bundle[]{systemBundle};
		}

		@Override
		public boolean isAssignableTo(Bundle bundle, String className) {
			// Since only one class loader is used, same class come from the same package
			return this.className.equals(className);
		}

		@Override
		@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EQ_COMPARETO_USE_OBJECT_EQUALS")
		public int compareTo(Object reference) {
			return (this == reference) ? 0 : 1;
		}

		@Override
		public Dictionary<String, Object> getProperties() {
			return new Hashtable<String, Object>(properties);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <A> A adapt(Class<A> type) {
			if (ServiceReferenceDTO.class.equals(type)) {
				return (A) DTOBuilder.newServiceReferenceDTO(this);
			}
			return null;
		}

	}

	/**
	 * Class name based service filter
	 * <p>
	 * Dictionary and capability look-ups are not supported and marked as deprecated.
	 */
	private class ClassNameBasedFilter implements Filter {

		private final static String NO_MATCH_CLASS_NAME = "";

		private final String className;

		private ClassNameBasedFilter(Optional<String> className) {
			this.className = className.orElse(NO_MATCH_CLASS_NAME);
		}

		@Override
		public boolean match(ServiceReference<?> reference) {
			return reference.isAssignableTo(systemBundle, className);
		}

		@Override
		@Deprecated
		public boolean match(Dictionary<String, ?> dictionary) {
			throw new UnsupportedOperationException("Dictionary based service look-up is not supported.");
		}

		@Override
		@Deprecated
		public boolean matchCase(Dictionary<String, ?> dictionary) {
			throw new UnsupportedOperationException("Dictionary based service look-up is not supported.");
		}

		@Override
		@Deprecated
		public boolean matches(Map<String, ?> map) {
			throw new UnsupportedOperationException("Capability based service look-up is not supported.");
		}

		@Override
		public String toString() {
			return className;
		}

	}

}
