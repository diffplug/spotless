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
package com.diffplug.spotless.extra.eclipse.base;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

import com.diffplug.spotless.extra.eclipse.base.osgi.BundleConfig;
import com.diffplug.spotless.extra.eclipse.base.osgi.BundleController;
import com.diffplug.spotless.extra.eclipse.base.runtime.PluginRegistrar;

/** Setup a framework for Spotless Eclipse based formatters */
public final class SpotlessEclipseFramework {
	/** Spotless demands for internal formatter chains Unix (LF) line endings. */
	public static final String LINE_DELIMITER = "\n";

	/**
	 * Default core bundles required by most plugins.
	 */
	public enum DefaultBundles {
		/**
		 * Plugins ask the platform whether core runtime bundle is in debug mode.
		 * <p>
		 * Note that the bundle requires the
		 * {@link org.eclipse.osgi.service.environment.EnvironmentInfo}
		 * service.
		 * </p>
		 * <p>
		 * Per default, the platform is not activated. Some plugins use this information
		 * to determine whether they are running in a headless mode (without IDE).
		 * </p>
		 */
		PLATFORM(org.eclipse.core.internal.runtime.PlatformActivator.class, Bundle.RESOLVED),
		/** The Spotless {@link BundleController} wraps the OSGi layer. But the plugin/extension look-up still uses the Eclipse OSGi registry.*/
		REGISTRY(org.eclipse.core.internal.registry.osgi.Activator.class),
		/** Eclipse preferences always check whether this bundle has been activated before preference are set.*/
		PREFERENCES(org.eclipse.core.internal.preferences.Activator.class),
		/** The common runtime provides common services, like log and service adapters registry. */
		COMMON(org.eclipse.core.internal.runtime.Activator.class);

		private final Class<? extends BundleActivator> activatorClass;
		private final int state;

		private DefaultBundles(Class<? extends BundleActivator> clazz) {
			this(clazz, Bundle.ACTIVE);
		}

		private DefaultBundles(Class<? extends BundleActivator> clazz, int state) {
			activatorClass = clazz;
			this.state = state;
		}

		/** Create new bundle activator instance. */
		public BundleActivator create() {
			return createInstance(activatorClass);
		}

		public int getDesiredState() {
			return state;
		}

		/** Create bundle activator instances for all enumerated values. */
		public static List<BundleConfig.Entry> createAll() {
			return Arrays.stream(values())
					.map(value -> new BundleConfig.Entry(value.create(), value.getDesiredState())).collect(Collectors.toList());
		}
	}

	/**
	 * Default plugins required by most Spotless formatters.
	 * <p>
	 * Eclipse plugins are OSGI bundles themselves and do not necessarily derive from the Eclipse Plugin class.
	 * {@link BundleActivator} implementation may as well serve as plugins.
	 * All plugins must provide a MANIFEST.MF, plugin.properties and plugin.xml description file,
	 * required for the plugin registration.
	 * </p>
	 */
	public enum DefaultPlugins {
		/**
		 * The resources plugin initialized the Eclipse workspace and allows URL look-up.
		 * Most formatters using the workspace to resolve URLs or create
		 * file interfaces.
		 * See {@code org.eclipse.core.resources.IFile} implementation for details.
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
		public static List<BundleActivator> createAll() {
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
	 * Creates and configures a new {@link SpotlessEclipseFramework} using
	 * {@link DefaultBundles}, {@link DefaultPlugins} and default {@link SpotlessEclipseServiceConfig}.
	 * If there is already a an instance, the call is ignored.
	 * @return False if the {@link SpotlessEclipseFramework} instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 * @deprecated  Use {@link #setup(SpotlessEclipseConfig)} instead.
	 */
	@Deprecated
	public synchronized static boolean setup() throws BundleException {
		return setup(plugins -> plugins.applyDefault());
	}

	/**
	 * Creates and configures a new {@link SpotlessEclipseFramework} using
	 * {@link DefaultBundles} and {@link DefaultPlugins}.
	 * If there is already a an instance, the call is ignored.
	 * @param plugins Eclipse plugins (which are also OSGi bundles) to start
	 * @return False if the {@link SpotlessEclipseFramework} instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 * @deprecated  Use {@link #setup(SpotlessEclipseConfig)} instead.
	 */
	@Deprecated
	public synchronized static boolean setup(Consumer<SpotlessEclipsePluginConfig> plugins) throws BundleException {
		return setup(config -> config.applyDefault(), plugins);
	}

	/**
	 * Creates and configures a new {@link SpotlessEclipseFramework} using {@link DefaultBundles}.
	 * If there is already a an instance, the call is ignored.
	 * @param config Framework service configuration
	 * @param plugins Eclipse plugins (which are also OSGi bundles) to start
	 * @return False if the {@link SpotlessEclipseFramework} instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 * @deprecated  Use {@link #setup(SpotlessEclipseConfig)} instead.
	 */
	@Deprecated
	public synchronized static boolean setup(Consumer<SpotlessEclipseServiceConfig> config, Consumer<SpotlessEclipsePluginConfig> plugins) throws BundleException {
		return setup(core -> core.applyDefault(), config, plugins);
	}

	/**
	 * Creates and configures a new {@link SpotlessEclipseFramework} if there is none.
	 * If there is already a an instance, the call is ignored.
	 * @param core Activators of core bundles
	 * @param services Framework service configuration
	 * @param plugins Eclipse plugins to start
	 * @return False if the {@link SpotlessEclipseFramework} instance already exists, true otherwise.
	 * @throws BundleException Throws exception in case the setup failed.
	 * @deprecated  Use {@link #setup(SpotlessEclipseConfig)} instead.
	 */
	@Deprecated
	public synchronized static boolean setup(Consumer<SpotlessEclipseCoreConfig> core, Consumer<SpotlessEclipseServiceConfig> services, Consumer<SpotlessEclipsePluginConfig> plugins) throws BundleException {
		if (null != INSTANCE) {
			return false;
		}
		setup(new SpotlessEclipseConfig() {
			@Override
			public void registerBundles(SpotlessEclipseCoreConfig config) {
				core.accept(config);
			}

			@Override
			public void registerServices(SpotlessEclipseServiceConfig config) {
				services.accept(config);
			}

			@Override
			public void activatePlugins(SpotlessEclipsePluginConfig config) {
				plugins.accept(config);
			}
		});
		return true;
	}

	/**
	 * Creates and configures a new {@link SpotlessEclipseFramework} if there is none.
	 * If there is already a an instance, the call is ignored.
	 * @param config Configuration
	 * @throws BundleException Throws exception in case the setup failed.
	 */
	public synchronized static void setup(SpotlessEclipseConfig config) throws BundleException {
		if (null == INSTANCE) {
			INSTANCE = new SpotlessEclipseFramework();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					INSTANCE.shutdown();
				} catch (Exception e) {
					//At shutdown everything is just done on best-efforts basis
				}
			}));

			config.registerServices(INSTANCE.getServiceConfig());

			SpotlessEclipseCoreConfig coreConfig = new SpotlessEclipseCoreConfig();
			config.registerBundles(coreConfig);
			INSTANCE.startCoreBundles(coreConfig);

			SpotlessEclipsePluginConfig pluginConfig = new SpotlessEclipsePluginConfig();
			config.activatePlugins(pluginConfig);
			for (Class<?> extension : pluginConfig.getExtensions()) {
				INSTANCE.addExtension(extension);
			}
			for (BundleConfig.Entry plugin : pluginConfig.get()) {
				INSTANCE.addPlugin(plugin.state, plugin.activator);
			}

			config.customize();
		}
	}

	private final Function<Bundle, BundleException> registry;
	private final BundleController controller;

	private SpotlessEclipseFramework() throws BundleException {

		controller = new BundleController();
		registry = (pluginBundle) -> {
			return PluginRegistrar.register(pluginBundle);
		};
	}

	void shutdown() {
		controller.shutdown();
	}

	private SpotlessEclipseServiceConfig getServiceConfig() {
		return controller.getServices();
	}

	private void addExtension(Class<?> clazzInBundleJar) throws BundleException {
		controller.addBundle(clazzInBundleJar, registry);
	}

	private void addPlugin(int state, BundleActivator plugin) throws BundleException {
		controller.addBundle(state, plugin, registry);
	}

	private void startCoreBundles(SpotlessEclipseCoreConfig coreConfig) throws BundleException {
		//The SAXParserFactory.class is required for parsing the plugin XML files
		addMandatoryServiceIfMissing(SAXParserFactory.class, SAXParserFactory.newInstance());

		/*
		 * Since org.eclipse.core.runtime version 3.15.300, the Eclipse bundle look-up is accomplished
		 * via the wiring framework, which requires starting the InternalPlatform.
		 * The internal platform initialization is customized by the services
		 * registered to the controller.
		 */
		InternalPlatform.getDefault().start(controller);

		for (BundleConfig.Entry coreBundle : coreConfig.get()) {
			try {
				BundleContext context = controller.createContext(coreBundle.state);
				coreBundle.activator.start(context);
			} catch (Exception e) {
				throw new BundleException(String.format("Failed to start %s", coreBundle.activator.getClass().getName()), BundleException.ACTIVATOR_ERROR, e);
			}
		}
	}

	private <S> void addMandatoryServiceIfMissing(Class<S> interfaceClass, S service) {
		if (null == controller.getServiceReference(interfaceClass)) {
			controller.getServices().add(interfaceClass, service);
		}
	}
}
