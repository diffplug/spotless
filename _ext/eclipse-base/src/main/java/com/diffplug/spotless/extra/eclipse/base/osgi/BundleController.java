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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.eclipse.core.internal.jobs.JobManager;
import org.eclipse.osgi.internal.location.LocationHelper;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.FrameworkWiring;

/**
 * OSGi bundle controller allowing a minimal Eclipse platform setup
 * by bypassing the Eclipse internal platform.
 *
 * Bundles are loaded by the same class loader, sharing the same context.
 * Services do not provide individual properties, but making use of the framework properties.
 * All bundles have a persistent state. The OSGi life-cycle is not supported.
 */
public final class BundleController implements StaticBundleContext {
	private static final String ECLIPSE_LAUNCHER_SYMBOLIC_NAME = "org.eclipse.osgi";

	private final SimpleBundle systemBundle;
	private final Map<String, String> properties;
	private final ServiceCollection services;
	private final BundleSet bundles;

	@SuppressWarnings("deprecation")
	public BundleController() throws BundleException {
		//OSGI locks are not required, since this framework does not allow changes after initialization.
		System.setProperty(LocationHelper.PROP_OSGI_LOCKING, LocationHelper.LOCKING_NONE);

		this.properties = new HashMap<String, String>();
		//Don't activate all plugin bundles. Activation is triggered by this controller where needed.
		properties.put(org.eclipse.core.internal.runtime.InternalPlatform.PROP_ACTIVATE_PLUGINS, Boolean.toString(false));

		/*
		 * Used to set-up an internal member of the Eclipse runtime FindSupport,
		 * which is used during resources look-up from different version of bundles.
		 * Since the concept of the Spotless Eclipse framework does not allow multiple versions
		 * for a bundle, this property is never used.
		 */
		properties.put(org.eclipse.core.internal.runtime.FindSupport.PROP_NL, "");

		bundles = new BundleSet();
		systemBundle = new SimpleBundle(this, Bundle.ACTIVE);
		bundles.add(systemBundle);

		services = new ServiceCollection(systemBundle, properties);

		//Eclipse core (InternalPlatform) still uses PackageAdmin for looking up bundles
		EclipseBundleLookup bundleLookup = new EclipseBundleLookup(systemBundle, bundles);
		services.add(org.osgi.service.packageadmin.PackageAdmin.class, bundleLookup);
		services.add(FrameworkWiring.class, bundleLookup);

		//Redirect framework activator requests to the org.eclipse.osgi bundle to this instance.
		bundles.add(new SimpleBundle(systemBundle, ECLIPSE_LAUNCHER_SYMBOLIC_NAME, Bundle.ACTIVE));
		FrameworkBundleRegistry.initialize(this);
	}

	/**
	 * Stop {@link org.eclipse.core.internal.jobs.JobManager} worker pool
	 * and clean up resources of Spotless bundles and services.
	 *
	 * @implNote The {@link org.osgi.framework.BundleActivator}s
	 * are not stopped, since they are not completely started.
	 * For example services are suppressed by {@link StaticBundleContext}.
	 */
	public void shutdown() {
		JobManager.shutdown();
		bundles.getAll().forEach(b -> {
			try {
				b.stop();
			} catch (IllegalStateException | BundleException e) {
				//Stop on best effort basis
			}
		});
		services.stop();
	}

	public ServiceCollection getServices() {
		return services;
	}

	/** Adds and starts a new bundle. */
	public void addBundle(int bundleState, BundleActivator activator, Function<Bundle, BundleException> register) throws BundleException {
		BundleContext contextFacade = new BundleControllerContextFacade(this, bundleState, activator);
		bundles.add(contextFacade.getBundle());
		BundleException exception = register.apply(contextFacade.getBundle());
		if (null != exception)
			throw exception;
		try {
			activator.start(contextFacade);
		} catch (Exception e) {
			throw new BundleException(String.format("Failed do start %s.", activator.getClass().getName()), BundleException.ACTIVATOR_ERROR, e);
		}
	}

	/** Adds new bundel whithout activator (e.g. used for only for extensions) */
	public void addBundle(Class<?> clazzInBundleJar, Function<Bundle, BundleException> register) throws BundleException {
		BundleContext contextFacade = new BundleControllerContextFacade(this, clazzInBundleJar);
		bundles.add(contextFacade.getBundle());
		BundleException exception = register.apply(contextFacade.getBundle());
		if (null != exception)
			throw exception;
	}

	/** Creates a context with an individual state if required. */
	public BundleContext createContext(int state) {
		if (state == systemBundle.getState()) {
			return this;
		}
		return new BundleControllerContextFacade(systemBundle, state);
	}

	@Override
	public String getProperty(String key) {
		return properties.get(key);
	}

	@Override
	public Bundle getBundle() {
		return systemBundle;
	}

	@Override
	public Bundle[] getBundles() {
		Collection<Bundle> shallowCopy = bundles.getAll();
		return shallowCopy.toArray(new Bundle[shallowCopy.size()]);
	}

	@Override
	public Bundle getBundle(long id) {
		return bundles.get(id);
	}

	@Override
	public Bundle getBundle(String location) {
		if (Constants.SYSTEM_BUNDLE_LOCATION.equals(location)) {
			return systemBundle;
		}
		return null;
	}

	@Override
	public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
		//Filters are based on class names
		var interfaceClassName = (null == clazz) ? filter : clazz;
		return services.getReferences(interfaceClassName);
	}

	@Override
	public <S> S getService(ServiceReference<S> reference) {
		return services.getService(reference);
	}

	@Override
	public Filter createFilter(String filter) throws InvalidSyntaxException {
		return services.createFilter(filter);
	}

	/**
	 * Facade providing access to an existing controller for a bundle
	 * <p>
	 * All bundles have unrestricted access to the framework services and properties.
	 * However, each bundle and its context needs to maintain its individual
	 * symbolic name for look-up.
	 * </p>
	 */
	private static class BundleControllerContextFacade implements StaticBundleContext {

		private final BundleContext context;
		private final Bundle bundle;

		private BundleControllerContextFacade(BundleContext context, int bundleState, BundleActivator activator) throws BundleException {
			this.context = context;
			bundle = new SimpleBundle(this, bundleState, activator);
		}

		private BundleControllerContextFacade(BundleContext context, Class<?> clazzInBundleJar) throws BundleException {
			this.context = context;
			bundle = new SimpleBundle(this, clazzInBundleJar);
		}

		/** Fakes an individual bundle state */
		private BundleControllerContextFacade(SimpleBundle bundle, int bundleState) {
			this.context = bundle.getBundleContext();
			this.bundle = new SimpleBundle(bundle, bundleState);
		}

		@Override
		public String getProperty(String key) {
			return context.getProperty(key);
		}

		@Override
		public Bundle getBundle() {
			return bundle;
		}

		@Override
		public Bundle[] getBundles() {
			return context.getBundles();
		}

		@Override
		public Bundle getBundle(long id) {
			return context.getBundle(id);
		}

		@Override
		public Bundle getBundle(String location) {
			return context.getBundle(location);
		}

		@Override
		public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
			return context.getAllServiceReferences(clazz, filter);
		}

		@Override
		public <S> S getService(ServiceReference<S> reference) {
			return context.getService(reference);
		}

		@Override
		public Filter createFilter(String filter) throws InvalidSyntaxException {
			return context.createFilter(filter);
		}

	}

}
