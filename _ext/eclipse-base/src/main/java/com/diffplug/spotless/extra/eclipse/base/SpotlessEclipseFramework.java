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
package com.diffplug.spotless.extra.eclipse.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParserFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;

import com.diffplug.spotless.extra.eclipse.base.osgi.BundleController;
import com.diffplug.spotless.extra.eclipse.base.runtime.PluginRegistrar;

/** Setup a framework for Spotless Eclipse based formatters */
public final class SpotlessEclipseFramework {

	/**
	 * Default internal bundles required by most plugins.
	 * <p>
	 * Services provided by an internal bundle are not accessed via the plugin registry, but by static methods.
	 * </p>
	 */
	public enum DefaultBundles {
		/** Plugins ask the platform whether core runtime bundle is in debug mode. Note that the bundle requires the EnvironmentInfo service. */
		PLATFORM(org.eclipse.core.internal.runtime.PlatformActivator.class),
		/** The Spotless BundleController wraps the OSGi layer. But the plugin/extension look-up still uses the Eclipse OSGi registry.*/
		REGISTRY(org.eclipse.core.internal.registry.osgi.Activator.class),
		/** Eclipse preferences always check whether this bundle has been activated before preference are set.*/
		PREFERENCES(org.eclipse.core.internal.preferences.Activator.class),
		/** The common runtime provides provides common services, like log and service adapters registry. */
		COMMON(org.eclipse.core.internal.runtime.Activator.class);

		private final Class<? extends BundleActivator> activatorClass;

		private DefaultBundles(Class<? extends BundleActivator> clazz) {
			activatorClass = clazz;
		}

		/** Create new bundle activator instance. */
		public BundleActivator create() {
			return createInstance(activatorClass);
		}

		/** Create bundle activator instances for all enumerated values. */
		public static Collection<BundleActivator> createAll() {
			return Arrays.stream(values()).map(value -> value.create()).collect(Collectors.toList());
		}
	}

	/**
	 * Default plugins required by most Spotless formatters.
	 * <p>
	 * Eclipse plugins are OSGI bundles itself and do not necessarily derive from the Eclipse Plugin class.
	 * BundleActivator implementation may as well server as plugins.
	 * All plugins must provide a MANIFEST.MF, plugin.properties and plugin.xml description file,
	 * required for the plugin registration.
	 * </p>
	 */
	public enum DefaultPlugins {
		/**
		 * The resources plugin initialized the Eclipse workspace and allows URL look-up.
		 * Most formatters using the workspace to resolve URLs or create
		 * file interfaces (org.eclipse.core.resources.IFile).
		 */
		RESOURCES(org.eclipse.core.resources.ResourcesPlugin.class);

		private final Class<? extends BundleActivator> pluginClass;

		DefaultPlugins(Class<? extends BundleActivator> clazz) {
			pluginClass = clazz;
		}

		/** Create new plugin instance. */
		public BundleActivator create() {
			return createInstance(pluginClass);
		}

		/** Create plugin instances for all enumerated values. */
		public static Collection<BundleActivator> createAll() {
			return Arrays.stream(values()).map(value -> value.create()).collect(Collectors.toList());
		}
	}

	private static <T> T createInstance(Class<? extends T> clazz) {
		try {
			Constructor<? extends T> ctor = clazz.getConstructor();
			return ctor.newInstance(new Object[]{});
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Failed to create instance for: " + clazz.getCanonicalName(), e);
		}
	}

	private static SpotlessEclipseFramework INSTANCE = null;

	/**
	 * Creates and configures a new SpotlessEclipseFramework using DefaultBundles, DefaultPlugins and default configuration.
	 * If there is already a an instance, the call is ignored.
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup() throws BundleException {
		return setup(plugins -> plugins.addAll(DefaultPlugins.createAll()));
	}

	/**
	 * Creates and configures a new SpotlessEclipseFramework using DefaultBundles and DefaultPlugins.
	 * If there is already a an instance, the call is ignored.
	 * @param plugins Eclipse plugins (which are also OSGi bundles) to start
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup(Consumer<Collection<BundleActivator>> plugins) throws BundleException {
		return setup(config -> config.applyDefault(), plugins);
	}

	/**
	 * Creates and configures a new SpotlessEclipseFramework using DefaultBundles.
	 * If there is already a an instance, the call is ignored.
	 * @param config Framework service configuration
	 * @param plugins Eclipse plugins (which are also OSGi bundles) to start
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup(Consumer<SpotlessEclipseServiceConfig> config, Consumer<Collection<BundleActivator>> plugins) throws BundleException {
		return setup(bundles -> bundles.addAll(DefaultBundles.createAll()), config, plugins);
	}

	/**
	 * Creates and configures a new SpotlessEclipseFramework if there is none.
	 * If there is already a an instance, the call is ignored.
	 * @param bundles Activators of internal bundles
	 * @param config Framework service configuration
	 * @param plugins Eclipse plugins to start
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup(Consumer<Collection<BundleActivator>> bundles, Consumer<SpotlessEclipseServiceConfig> config, Consumer<Collection<BundleActivator>> plugins) throws BundleException {
		if (null != INSTANCE) {
			return false;
		}

		Collection<BundleActivator> internalBundleActivators = new ArrayList<BundleActivator>();
		bundles.accept(internalBundleActivators);
		INSTANCE = new SpotlessEclipseFramework(internalBundleActivators);
		config.accept(INSTANCE.getServiceConfig());

		Collection<BundleActivator> pluginsList = new ArrayList<BundleActivator>();
		plugins.accept(pluginsList);
		for (BundleActivator plugin : pluginsList) {
			INSTANCE.addPlugin(plugin);
		}
		return true;
	}

	private final Function<Bundle, BundleException> registry;
	private final BundleController controller;
	private final Collection<BundleActivator> bundleActivators;
	private boolean bundleActivatorsStarted;

	private SpotlessEclipseFramework(Collection<BundleActivator> bundleActivators) throws BundleException {

		controller = new BundleController();
		registry = (pluginBundle) -> {
			return PluginRegistrar.register(pluginBundle);
		};

		this.bundleActivators = bundleActivators;
		bundleActivatorsStarted = false;
	}

	/** Get framework service configuration */
	private SpotlessEclipseServiceConfig getServiceConfig() {
		return controller.getServices();
	}

	/** Add a plugin to the framework. */
	private void addPlugin(BundleActivator plugin) throws BundleException {
		if (!bundleActivatorsStarted) {
			//The SAXParserFactory.class is required for parsing the plugin XML files
			addMandatoryServiceIfMissing(SAXParserFactory.class, SAXParserFactory.newInstance());
			startFrameworkBundles();
			bundleActivatorsStarted = true;
		}
		controller.addBundle(plugin, registry);
	}

	private <S> void addMandatoryServiceIfMissing(Class<S> interfaceClass, S service) {
		if (null == controller.getServiceReference(interfaceClass)) {
			controller.getServices().add(interfaceClass, service);
		}
	}

	private void startFrameworkBundles() throws BundleException {
		for (BundleActivator activator : bundleActivators) {
			try {
				activator.start(controller);
			} catch (Exception e) {
				throw new BundleException(String.format("Failed to start %s", activator.getClass().getName()), BundleException.ACTIVATOR_ERROR, e);
			}
		}
	}

}
