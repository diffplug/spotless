/*
 * Copyright 2016 DiffPlug
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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Objects;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Restriction of the BundleContext interface rejecting run-time provision of bundles.
 * Services provided at run-time are ignored.
 * <p>
 * Multiple service instances per class are not supported, hence the services can be filtered by class name.
 * Unsupported methods are marked as deprecated an causing an exception.
 * Registration and removal of bundle/service listeners are ignored, since a run-time
 * provision of bundles or services is not supported.
 */
interface StaticBundleContext extends BundleContext {

	@Override
	@Deprecated
	default public Bundle installBundle(String location, InputStream input) throws BundleException {
		throw new UnsupportedOperationException("Run-time installation of bundles is not supported.");
	}

	@Override
	@Deprecated
	default public Bundle installBundle(String location) throws BundleException {
		throw new UnsupportedOperationException("Run-time installation of bundles is not supported.");
	}

	@Override
	default public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {}

	@Override
	default public void addServiceListener(ServiceListener listener) {}

	@Override
	default public void removeServiceListener(ServiceListener listener) {}

	@Override
	default public void addBundleListener(BundleListener listener) {}

	@Override
	default public void removeBundleListener(BundleListener listener) {}

	@Override
	default public void addFrameworkListener(FrameworkListener listener) {}

	@Override
	default public void removeFrameworkListener(FrameworkListener listener) {}

	@Override
	@Deprecated
	default public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
		throw new UnsupportedOperationException("Run-time provision of services is not supported.");
	}

	@Override
	@Deprecated
	default public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
		return null; //Ignore additional services
	}

	@Deprecated
	@Override
	default public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
		return null; //Ignore additional services
	}

	@Override
	@Deprecated
	default public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
		return null; //Ignore additional services
	}

	@SuppressWarnings("unchecked")
	@Override
	default public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
		Objects.requireNonNull(clazz, "The class under whose name the service was registered must not be null.");
		return (ServiceReference<S>) getServiceReference(clazz.getName());
	}

	@Override
	default public ServiceReference<?> getServiceReference(String clazz) {
		Objects.requireNonNull(clazz, "The class under whose name the service was registered must not be null.");
		ServiceReference<?>[] references;
		try {
			references = getServiceReferences(clazz, null);
		} catch (InvalidSyntaxException e) {
			throw new RuntimeException(e); //null is always valid
		}
		return (null == references) ? null : references[0];
	}

	@Override
	default public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		return getAllServiceReferences(clazz, filter); //Services are always considered compatible
	}

	@Override
	default public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		//Filters are based on class names
		ServiceReference<?> reference = (null == clazz) ? getServiceReference(filter) : getServiceReference(clazz);
		return (reference == null) ? null : new ServiceReference<?>[]{reference};
	}

	@SuppressWarnings("unchecked")
	@Override
	default public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
		ServiceReference<?>[] references = getServiceReferences(clazz.getName(), filter);
		Collection<ServiceReference<S>> result = new ArrayList<ServiceReference<S>>(0);
		if (null != references) {
			result = Arrays.stream(references).map(r -> (ServiceReference<S>) r).collect(Collectors.toList());
		}
		return result;
	}

	@Override
	default public boolean ungetService(ServiceReference<?> reference) {
		return true; //Services are persistent and never unregistered
	}

	@Override
	@Deprecated
	default public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
		throw new UnsupportedOperationException("Service specific objects are not supported.");
	}

	@Override
	@Deprecated
	default public File getDataFile(String filename) {
		throw new UnsupportedOperationException("Persistent data storage provision is handled by the Location service.");
	}

}
