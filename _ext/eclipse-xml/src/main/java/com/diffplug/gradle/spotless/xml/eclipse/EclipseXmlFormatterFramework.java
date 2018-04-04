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
package com.diffplug.gradle.spotless.xml.eclipse;

import static com.diffplug.gradle.spotless.xml.eclipse.EclipseXmlFormatterPreferenceNames.*;
import static org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.environment.EnvironmentInfo;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.Catalog;
import org.eclipse.wst.xml.core.internal.catalog.CatalogReader;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.formatter.XMLFormattingPreferences;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceInitializer;
import org.eclipse.xsd.util.XSDSchemaBuildingTools;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleException;

import com.diffplug.gradle.spotless.xml.eclipse.adapter.PluginRegistrar;
import com.diffplug.gradle.spotless.xml.eclipse.osgi.BundleController;
import com.diffplug.gradle.spotless.xml.eclipse.osgi.ServiceCollection;
import com.diffplug.gradle.spotless.xml.eclipse.service.*;

/** Framework required by Eclipse XML formatter step */
class EclipseXmlFormatterFramework {

	private static EclipseXmlFormatterFramework INSTANCE = null;

	public static synchronized XMLFormattingPreferences setup(Properties properties) throws BundleException {
		if (null == INSTANCE) {
			INSTANCE = new EclipseXmlFormatterFramework();
		}
		return INSTANCE.parseConfig(properties);
	}

	private final Plugin[] plugins = {
			//The WST uses the workspace to resolve file URIs
			new org.eclipse.core.resources.ResourcesPlugin(),
			//The WST XML formatter
			new org.eclipse.wst.xml.core.internal.XMLCorePlugin(),
			//XSDs/DTDs must be resolved by URI
			new org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin(),
			//Support formatting based on DTD restrictions
			new org.eclipse.wst.dtd.core.internal.DTDCorePlugin(),
			//Support formatting based on XSD restrictions
			new org.eclipse.wst.xsd.core.internal.XSDCorePlugin()
	};

	private final XmlFormattingPreferencesFactory preferencesFactory;
	private final Function<Bundle, BundleException> registry;
	private final BundleController controller;

	private EclipseXmlFormatterFramework() throws BundleException {
		//Eclipse framework
		BundleActivator[] framework = {
				//Plugins ask the platform whether core runtime bundle is in debug mode
				new org.eclipse.core.internal.runtime.PlatformActivator(),
				//The BundleController wraps the OSGi layer. The plugin/extension look-up still works via the registry.
				new org.eclipse.core.internal.registry.osgi.Activator(),
				//Preferences always check whether bundle has been activated before preference are been set.
				new org.eclipse.core.internal.preferences.Activator(),
				//Workspace is used for resolving URIs. Though the look-up will not be successful,
				//since no projects are available, the look-up must be supported.
				new org.eclipse.core.internal.runtime.Activator()
		};

		//Eclipse services
		ServiceCollection services = new ServiceCollection();
		services.add(DebugOptions.class, new NoDebugging());
		services.add(EnvironmentInfo.class, new EmptyEnvironment());
		services.add(SAXParserFactory.class, SAXParserFactory.newInstance());
		services.add(Location.class, new TemporaryLocation());
		services.add(IContentTypeManager.class, new NoContentTypeSpecificHandling());
		services.add(IPreferencesService.class, new NoEclipsePreferences());

		controller = new BundleController(services, framework);
		registry = (pluginBundle) -> {
			return PluginRegistrar.register(pluginBundle);
		};
		for (Plugin plugin : plugins) {
			controller.addBundle(plugin, registry);
		}
		//Register required EMF factories
		XSDSchemaBuildingTools.getXSDFactory();

		preferencesFactory = new XmlFormattingPreferencesFactory();
	}

	private XMLFormattingPreferences parseConfig(Properties config) {
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

		return preferencesFactory.create(config);
	}

	private static Catalog getDefaultCatalog() {
		ICatalog defaultCatalogI = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
		if (defaultCatalogI instanceof Catalog) {
			return (Catalog) defaultCatalogI;
		}
		throw new IllegalArgumentException("Internal error: Catalog implementation '" + defaultCatalogI.getClass().getCanonicalName() + "' unsupported.");
	}

	private Optional<File> getCatalogConfig(Properties config) {
		String newLocation = config.getProperty(USER_CATALOG);
		if (newLocation != null && !newLocation.isEmpty()) {
			return Optional.of(new File(newLocation));
		}
		return Optional.empty();
	}

	/**
	 * Provides XmlFormattingPreferences factory.
	 * <p>
	 * Note that multiple preference setups (for example project specific once)
	 * are not supported by the WST plugin. But the XML formatting preferences
	 * are independent from the plugin. Hence multiple formatter preference instances can be created.
	 */
	private static class XmlFormattingPreferencesFactory {
		private final static List<String> SUPPORTED_XML_FORMAT_PREFS = Arrays.asList(
				FORMAT_COMMENT_TEXT,
				FORMAT_COMMENT_JOIN_LINES,
				LINE_WIDTH,
				SPLIT_MULTI_ATTRS,
				ALIGN_END_BRACKET,
				SPACE_BEFORE_EMPTY_CLOSE_TAG,
				PRESERVE_CDATACONTENT,
				INDENTATION_CHAR,
				INDENTATION_SIZE,
				CLEAR_ALL_BLANK_LINES);

		private XmlFormattingPreferencesFactory() {
			XMLCorePreferenceInitializer initializer = new XMLCorePreferenceInitializer();
			initializer.initializeDefaultPreferences();
		}

		private XMLFormattingPreferences create(final Properties properties) {
			Properties newXmlProperties = getXMLFormattingProperties(properties);
			Properties defaultXmlProperties = setGlobalPreferences(newXmlProperties);
			XMLFormattingPreferences xmlPreferences = new XMLFormattingPreferences();
			setGlobalPreferences(defaultXmlProperties);
			return xmlPreferences;
		}

		private Properties getXMLFormattingProperties(final Properties properties) {
			Properties xmlFormattingPropertiesProperties = new Properties();
			properties.entrySet().stream().filter(
					entry -> SUPPORTED_XML_FORMAT_PREFS.contains(entry.getKey())).forEach(entry -> xmlFormattingPropertiesProperties.put(entry.getKey(), entry.getValue()));
			return xmlFormattingPropertiesProperties;
		}

		private Properties setGlobalPreferences(Properties newValues) {
			IEclipsePreferences globalPreferences = DefaultScope.INSTANCE.getNode(XMLCorePlugin.getDefault().getBundle().getSymbolicName());
			Properties oldValues = new Properties();
			newValues.forEach((key, value) -> {
				oldValues.put(key, globalPreferences.get((String) key, null));
				globalPreferences.put((String) key, (String) value);
			});
			return oldValues;
		}

	}

}
