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
package com.diffplug.spotless.extra.eclipse.wtp.sse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.Catalog;
import org.eclipse.wst.xml.core.internal.catalog.CatalogReader;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;

/** Adaptations of the Eclipse WTP environment for Spotless. */
public class SpotlessPreferences {
	/**
	 * Optional XML catalog for XSD/DTD lookup.
	 * Catalog versions 1.0 and 1.1 are supported.
	 * <p>
	 * Value is of type {@code Path}.
	 * </p>
	 */
	public static final String USER_CATALOG = "userCatalog";

	/** Configures the Eclipse properties for a Plugin and returns its previous values. */
	public static Properties configurePluginPreferences(Plugin plugin, Properties newValues) {
		IEclipsePreferences globalPreferences = DefaultScope.INSTANCE.getNode(plugin.getBundle().getSymbolicName());
		Properties oldValues = new Properties();
		newValues.forEach((key, value) -> {
			String oldValue = globalPreferences.get((String) key, null);
			if (null != oldValue) {
				oldValues.put(key, oldValue);
			}
			globalPreferences.put((String) key, (String) value);
		});
		return oldValues;
	}

	public static void configureCatalog(final Properties config) {
		Optional<File> catalog = getCatalogConfig(config);
		Catalog defaultCatalog = getDefaultCatalog();
		if (catalog.isPresent()) {
			try {
				InputStream inputStream = new FileInputStream(catalog.get());
				String orgBase = defaultCatalog.getBase();
				defaultCatalog.setBase(catalog.get().toURI().toString());
				CatalogReader.read((Catalog) defaultCatalog, inputStream);
				defaultCatalog.setBase(orgBase);
			} catch (IOException e) {
				throw new IllegalArgumentException(
						String.format("Value of '%s' refers to '%s', which cannot be read.", USER_CATALOG, catalog.get()));
			}
		} else {
			defaultCatalog.clear();
		}
	}

	private static Catalog getDefaultCatalog() {
		ICatalog defaultCatalogI = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
		if (defaultCatalogI instanceof Catalog) {
			return (Catalog) defaultCatalogI;
		}
		throw new IllegalArgumentException("Internal error: Catalog implementation '" + defaultCatalogI.getClass().getCanonicalName() + "' unsupported.");
	}

	private static Optional<File> getCatalogConfig(Properties config) {
		String newLocation = config.getProperty(USER_CATALOG);
		if (newLocation != null && !newLocation.isEmpty()) {
			return Optional.of(new File(newLocation));
		}
		return Optional.empty();
	}

}
