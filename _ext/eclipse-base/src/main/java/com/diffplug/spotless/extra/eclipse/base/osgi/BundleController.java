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
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.osgi.storage.bundlefile.BundleEntry;
import org.eclipse.osgi.storage.bundlefile.BundleFile;
import org.eclipse.osgi.storage.bundlefile.DirBundleFile;
import org.eclipse.osgi.storage.bundlefile.ZipBundleFile;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

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

	private final Bundle systemBundle;
	private final Map<String, String> properties;
	private final ServiceCollection services;
	private final BundleSet bundles;

	@SuppressWarnings("deprecation")
	public BundleController() throws BundleException {
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
		systemBundle = new SystemBundle(this);
		bundles.add(systemBundle);

		services = new ServiceCollection(systemBundle, properties);
		//Eclipse core (InternalPlatform) still uses PackageAdmin for looking up bundles
		services.add(org.osgi.service.packageadmin.PackageAdmin.class, new EclipseBundleLookup(bundles));

		//Redirect framework activator requests to the the org.eclipse.osgi bundle to this instance.
		bundles.add(new SystemBundle(ECLIPSE_LAUNCHER_SYMBOLIC_NAME, Bundle.ACTIVE, this));
	}

	public ServiceCollection getServices() {
		return services;
	}

	/** Adds and starts a new bundle. */
	public void addBundle(BundleActivator activator, Function<Bundle, BundleException> register) throws BundleException {
		BundleContext contextFacade = new BundleControllerContextFacade(activator);
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
		String interfaceClassName = (null == clazz) ? filter : clazz;
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

	/** Fixed state system bundle. */
	private static class SystemBundle implements StaticBundle, TemporaryBundle {
		private final String name;
		private final int state;
		private final BundleContext context;
		private final int id;
		private final BundleFile bundleFile;
		/**
		 * Spotless Eclipse formatters using in many cases fat JARs, compressing multiple
		 * bundles within one JAR. Their resources can get overridden in a fat JAR.
		 * Hence they provided under the bundle activators path name.
		 */
		private final String fatJarResourcePath;

		/** Active system bundle corresponding to the local JARs manifest */
		private SystemBundle(BundleContext bundleController) throws BundleException {
			this(null, null, Bundle.ACTIVE, bundleController);
		}

		/** System bundle with a custom fixed state and symbolic name */
		private SystemBundle(String symbolicName, int fixedState, BundleContext bundleController) throws BundleException {
			this(symbolicName, null, fixedState, bundleController);
		}

		/** Internal initialization of system bundle. */
		private SystemBundle(String symbolicName, BundleActivator activator, int fixedState, BundleContext bundleController) throws BundleException {
			state = fixedState;
			context = bundleController;
			id = context.getBundles().length;
			Object bundleObj = (null != activator) ? activator : this;
			String bundleObjPath = bundleObj.getClass().getName();
			fatJarResourcePath = bundleObjPath.substring(0, bundleObjPath.lastIndexOf('.'));
			try {
				bundleFile = getBundlFile(bundleObj);
				name = (null != symbolicName) ? symbolicName : getSymbolicName(getEntry(JarFile.MANIFEST_NAME));
				if (null == name) {
					throw new BundleException(String.format("No resource URL found for '%s'. Cannot determine bundle name of '%s'.", JarFile.MANIFEST_NAME, bundleObjPath));
				}
			} catch (BundleException e) {
				throw new BundleException(String.format("Failed to load bundle for '%s'.", bundleObjPath), e);
			}
		}

		@Override
		public int getState() {
			return state;
		}

		@Override
		public long getBundleId() {
			return id;
		}

		@Override
		public ServiceReference<?>[] getRegisteredServices() {
			try {
				return context.getAllServiceReferences(null, null);
			} catch (InvalidSyntaxException e) {
				throw new RuntimeException(e); //Filter 'null' is valid for 'select all'.
			}
		}

		@Override
		public String getSymbolicName() {
			return name;
		}

		@Override
		public BundleContext getBundleContext() {
			return context;
		}

		@Override
		public Enumeration<String> getEntryPaths(String path) {
			return bundleFile.getEntryPaths(path);
		}

		@Override
		public URL getEntry(String path) {
			URL result = tryFatJarMetaInf(path);
			if (null == result) {
				BundleEntry entry = bundleFile.getEntry(path);
				result = null == entry ? null : entry.getLocalURL();
			}
			return result;
		}

		private URL tryFatJarMetaInf(String path) {
			return getResource(fatJarResourcePath + "/" + path);
		}

		private static String getSymbolicName(URL manifestUrl) throws BundleException {
			if (null != manifestUrl) {
				try {
					Manifest manifest = new Manifest(manifestUrl.openStream());
					String headerValue = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
					if (null == headerValue) {
						throw new BundleException(String.format("Symbolic values not found in '%s'.", manifestUrl), BundleException.MANIFEST_ERROR);
					}
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, headerValue);
					if (null == elements) {
						throw new BundleException(String.format("Symbolic name not found in '%s'.", headerValue), BundleException.MANIFEST_ERROR);
					}
					//The parser already checked that at least one value exists
					return elements[0].getValueComponents()[0];

				} catch (IOException e) {
					throw new BundleException(String.format("Failed to parse Manifest '%s'.", manifestUrl), BundleException.MANIFEST_ERROR, e);
				}
			}
			return null;
		}

		private static BundleFile getBundlFile(Object obj) throws BundleException {
			URL objUrl = obj.getClass().getProtectionDomain().getCodeSource().getLocation();
			File jarOrDirectory = new File(objUrl.getPath());
			if (!(jarOrDirectory.exists() && jarOrDirectory.canRead())) {
				throw new BundleException(String.format("Path '%s' for '%s' is not accessible exist on local file system.", objUrl, obj.getClass().getName()), BundleException.READ_ERROR);
			}
			try {
				return jarOrDirectory.isDirectory() ? new DirBundleFile(jarOrDirectory, false) : new ZipBundleFile(jarOrDirectory, null, null, null);
			} catch (IOException e) {
				throw new BundleException(String.format("Cannot access bundle at '%s'.", jarOrDirectory), BundleException.READ_ERROR);
			}
		}

	}

	/**
	 * Additional bundle provided in JAR file.
	 * <p>
	 * The JAR file is determined by the location of the plugin / bundle activator location.
	 */
	private static class AdditionalBundle extends SystemBundle {

		private AdditionalBundle(BundleActivator activator, BundleContext bundleContext) throws BundleException {
			super(null, activator, Bundle.ACTIVE, bundleContext);
		}

	}

	/**
	 * Facade providing access to bundle controller
	 * <p>
	 * All bundles have unrestricted access to the framework services and properties.
	 * However, each bundle and its context needs to maintain its individual
	 * symbolic name for look-up.
	 */
	private class BundleControllerContextFacade implements StaticBundleContext {

		private final Bundle bundle;

		private BundleControllerContextFacade(BundleActivator activator) throws BundleException {
			this.bundle = new AdditionalBundle(activator, this);
		}

		@Override
		public String getProperty(String key) {
			return BundleController.this.getProperty(key);
		}

		@Override
		public Bundle getBundle() {
			return bundle;
		}

		@Override
		public Bundle[] getBundles() {
			return BundleController.this.getBundles();
		}

		@Override
		public Bundle getBundle(long id) {
			return BundleController.this.getBundle(id);
		}

		@Override
		public Bundle getBundle(String location) {
			return BundleController.this.getBundle(location);
		}

		@Override
		public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
			return BundleController.this.getAllServiceReferences(clazz, filter);
		}

		@Override
		public <S> S getService(ServiceReference<S> reference) {
			return BundleController.this.getService(reference);
		}

		@Override
		public Filter createFilter(String filter) throws InvalidSyntaxException {
			return BundleController.this.createFilter(filter);
		}

	}

}
