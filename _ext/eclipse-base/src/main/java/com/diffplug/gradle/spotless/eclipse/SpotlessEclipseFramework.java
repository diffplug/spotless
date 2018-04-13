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
package com.diffplug.gradle.spotless.eclipse;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;

import com.diffplug.gradle.spotless.eclipse.osgi.BundleController;
import com.diffplug.gradle.spotless.eclipse.runtime.PluginRegistrar;

/** Setup a framework for Spotless Eclipse based formatters */
public final class SpotlessEclipseFramework {

	/** Default plugins required by most Spotless formatters */
	public enum DefaultPlugins {
		/**
		 * The resources plugin initialized the Eclipse workspace and allows URL look-up.
		 * Most formatters using the workspace to resolve URLs or create
		 * file interfaces (org.eclipse.core.resources.IFile).
		 */
		RESOURCES(org.eclipse.core.resources.ResourcesPlugin.class);

		private final Class<? extends Plugin> pluginClass;

		DefaultPlugins(Class<? extends Plugin> clazz) {
			pluginClass = clazz;
		}

		/** Create new plugin instance. */
		public Plugin create() {
			return createInstance(pluginClass);
		}

		/** Create plugin instances for all enumerated values. */
		public static Plugin[] createValues() {
			List<Plugin> instances = Arrays.stream(values()).map(value -> value.create()).collect(Collectors.toList());
			return instances.toArray(new Plugin[0]);
		}
	}

	/** Default internal bundles/services required by most plugins */
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
		public static BundleActivator[] createValues() {
			List<BundleActivator> instances = Arrays.stream(values()).map(value -> value.create()).collect(Collectors.toList());
			return instances.toArray(new BundleActivator[0]);
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
	 * Creates and configures a new SpotlessEclipseFramework using DefaultBundles, DefaultPlugins and default services.
	 * If there is already a an instance, the call is ignored.
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup() throws BundleException {
		return setup(config -> {
			config.disableDebugging();
			config.hideEnvironment();
			config.ignoreContentType();
			config.ignoreUnsupportedPreferences();
			config.useTemporaryLocations();
		});
	}

	/**
	 * Creates and configures a new SpotlessEclipseFramework using DefaultBundles and DefaultPlugins.
	 * If there is already a an instance, the call is ignored.
	 * @param config Framework service configuration
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup(final Consumer<SpotlessEclipseServiceConfig> config) throws BundleException {
		return setup(DefaultPlugins.createValues(), config);
	}

	/**
	 * Creates and configures a new SpotlessEclipseFramework using DefaultBundles.
	 * If there is already a an instance, the call is ignored.
	 * @param plugins Eclipse plugins (which are also OSGi bundles) to start
	 * @param config Framework service configuration
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup(Plugin[] plugins, final Consumer<SpotlessEclipseServiceConfig> config) throws BundleException {
		return setup(DefaultBundles.createValues(), plugins, config);
	}

	/**
	 * Creates and configures a new SpotlessEclipseFramework if there is none.
	 * If there is already a an instance, the call is ignored.
	 * @param bundleActivators Activators of internal bundles
	 * @param plugins Eclipse plugins (which are also OSGi bundles) to start
	 * @param config Framework service configuration
	 * @return False if the SpotlessEclipseFramework instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static boolean setup(BundleActivator[] bundleActivators, Plugin[] plugins, final Consumer<SpotlessEclipseServiceConfig> config) throws BundleException {
		if (null != INSTANCE) {
			return false;
		}
		INSTANCE = new SpotlessEclipseFramework(bundleActivators);
		config.accept(INSTANCE.getServiceConfig());
		for (Plugin plugin : plugins) {
			INSTANCE.addPlugin(plugin);
		}
		return true;
	}

	private final Function<Bundle, BundleException> registry;
	private final BundleController controller;
	private final BundleActivator[] bundleActivators;
	private boolean bundleActivatorsStarted;

	private SpotlessEclipseFramework(BundleActivator[] bundleActivators) throws BundleException {

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
	private void addPlugin(Plugin plugin) throws BundleException {
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
