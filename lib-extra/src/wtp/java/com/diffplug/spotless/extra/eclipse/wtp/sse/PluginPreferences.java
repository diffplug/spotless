/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.extra.eclipse.wtp.sse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.wst.xml.core.internal.catalog.Catalog;
import org.eclipse.wst.xml.core.internal.catalog.CatalogReader;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;

/**
 * The plugin preference configuration of most WTP formatters is accomplished via the
 * globabl Eclipse preference lookup.
 * Spotless allows different formatter configurations per sub-projects.
 * Fortunately most formatters only perform a lookup on instantiation and not afterwards.
 * This is sometimes not true for all preferences of the formatter.
 *
 *
 */
public class PluginPreferences {
	/**
	 * Optional XML catalog for XSD/DTD lookup.
	 * Catalog versions 1.0 and 1.1 are supported.
	 * <p>
	 * Value is of type {@code Path}.
	 * </p>
	 */
	public static final String USER_CATALOG = "userCatalog";

	/**
	 * Indicates if external URIs (location hints) should be resolved
	 * and the referenced DTD/XSD shall be applied. Per default
	 * external URIs are ignored.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String RESOLVE_EXTERNAL_URI = "resolveExternalURI";

	/** Storage of latest global configuration */
	private static final Map<String, Properties> CONFIG = new HashMap<>();

	/** \return True if user explicitly set the  */
	public static boolean isExternalUriAllowed(Properties properties) {
		return Boolean.parseBoolean(properties.getProperty(PluginPreferences.RESOLVE_EXTERNAL_URI, "false"));
	}

	/** Configures persistent Eclipse properties */
	public static void configure(Plugin plugin, AbstractPreferenceInitializer defaultInitializer, Properties properties) {
		defaultInitializer.initializeDefaultPreferences();
		IEclipsePreferences globalPreferences = DefaultScope.INSTANCE.getNode(getPluginId(plugin));
		properties.forEach((key, value) -> {
			globalPreferences.put((String) key, (String) value);
		});
		store(plugin, properties);
	}

	/** Stores Eclipse properties for later comparison */
	public static void store(Plugin plugin, Properties properties) {
		CONFIG.put(getPluginId(plugin), properties);
	}

	/** Keep in mind that the ID is e.g. equal for all plugins. So assure that all user properties ge stored. */
	private static String getPluginId(Plugin plugin) {
		return plugin.getBundle().getSymbolicName();
	}

	public static void configureCatalog(final Properties properties, final ICatalog defaultCatalogInterface) {
		Objects.requireNonNull(properties, "Property values are missing.");
		Objects.requireNonNull(defaultCatalogInterface, "Default catalog missing.");
		if (!(defaultCatalogInterface instanceof Catalog)) {
			throw new IllegalArgumentException("Internal error: Catalog implementation '" + defaultCatalogInterface.getClass().getCanonicalName() + "' unsupported.");
		}
		Catalog defaultCatalog = (Catalog) defaultCatalogInterface;
		String catalogProperty = properties.getProperty(USER_CATALOG, "");
		if (!catalogProperty.isEmpty()) {
			final File catalogFile = new File(catalogProperty);
			try {
				InputStream inputStream = new FileInputStream(catalogFile);
				String orgBase = defaultCatalog.getBase();
				defaultCatalog.setBase(catalogFile.toURI().toString());
				CatalogReader.read((Catalog) defaultCatalog, inputStream);
				defaultCatalog.setBase(orgBase);
			} catch (IOException e) {
				throw new IllegalArgumentException(
						String.format("Value of '%s' refers to '%s', which cannot be read.", USER_CATALOG, catalogFile));
			}
		} else {
			defaultCatalog.clear();
		}
	}

	/** Throws exception in case configuration has changed */
	public static void assertNoChanges(Plugin plugin, Properties properties) {
		Objects.requireNonNull(properties, "Property values are missing.");
		final String preferenceId = plugin.getBundle().getSymbolicName();
		Properties originalValues = CONFIG.get(preferenceId);
		if (null == originalValues) {
			throw new IllegalArgumentException("No configuration found for " + preferenceId);
		}
		if (!originalValues.equals(properties)) {
			throw new IllegalArgumentException("The Eclipse formatter does not support multiple configurations.");
		}
	}

}
