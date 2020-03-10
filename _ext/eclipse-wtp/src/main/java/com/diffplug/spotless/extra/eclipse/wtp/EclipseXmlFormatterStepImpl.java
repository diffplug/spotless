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
package com.diffplug.spotless.extra.eclipse.wtp;

import static com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework.LINE_DELIMITER;
import static org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceNames.*;

import java.util.Properties;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin;
import org.eclipse.wst.dtd.core.internal.DTDCorePlugin;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.text.BasicStructuredDocument;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.Catalog;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.document.DOMModelImpl;
import org.eclipse.wst.xml.core.internal.formatter.DefaultXMLPartitionFormatter;
import org.eclipse.wst.xml.core.internal.formatter.XMLFormattingPreferences;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryAdapterFactoryForXML;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.parser.XMLSourceParser;
import org.eclipse.wst.xml.core.internal.preferences.XMLCorePreferenceInitializer;
import org.eclipse.wst.xml.core.internal.text.rules.StructuredTextPartitionerForXML;
import org.eclipse.wst.xsd.core.internal.XSDCorePlugin;
import org.eclipse.xsd.util.XSDSchemaBuildingTools;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipsePluginConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseServiceConfig;
import com.diffplug.spotless.extra.eclipse.wtp.sse.PluginPreferences;
import com.diffplug.spotless.extra.eclipse.wtp.sse.PreventExternalURIResolverExtension;

/** Formatter step which calls out to the Eclipse XML formatter. */
public class EclipseXmlFormatterStepImpl {
	private final DefaultXMLPartitionFormatter formatter;
	private final XMLFormattingPreferences preferences;
	private final INodeAdapterFactory xmlAdapterFactory;

	public EclipseXmlFormatterStepImpl(Properties properties) throws Exception {
		SpotlessEclipseFramework.setup(new FrameworkConfig(properties));
		PluginPreferences.assertNoChanges(XMLCorePlugin.getDefault(), properties);
		preferences = new XMLFormattingPreferences();
		formatter = new DefaultXMLPartitionFormatter();
		//The adapter factory maintains the common CMDocumentCache
		xmlAdapterFactory = new ModelQueryAdapterFactoryForXML();
	}

	static class FrameworkConfig implements SpotlessEclipseConfig {
		private final Properties properties;

		FrameworkConfig(Properties properties) {
			/*
			 * The cache is only used for system catalogs, but not for user catalogs.
			 * It requires the SSECorePLugin, which has either a big performance overhead,
			 * or needs a dirty mocking (we don't really require its functions but it needs to be there).
			 * So we disable the cache for now.
			 * This might cause a performance drop in case for example XSDs are formatted.
			 * But these standard/system restriction files contain anyway no formatting rules.
			 * So in case of performance inconveniences, we could add a Spotless property to disable the
			 * XSD/DTD parsing entirely (for example by adding an own URI resolver).
			 */
			properties.setProperty(CMDOCUMENT_GLOBAL_CACHE_ENABLED, Boolean.toString(false));
			this.properties = properties;
		}

		@Override
		public void registerServices(SpotlessEclipseServiceConfig config) {
			config.applyDefault();
			config.useSlf4J(EclipseXmlFormatterStepImpl.class.getPackage().getName());
		}

		@Override
		public void activatePlugins(SpotlessEclipsePluginConfig config) {
			config.applyDefault();
			activateXmlPlugins(config, PluginPreferences.isExternalUriAllowed(properties));
		}

		static void activateXmlPlugins(SpotlessEclipsePluginConfig config, boolean allowExternalURI) {
			//The WST XML formatter
			config.add(new XMLCorePlugin());
			//XSDs/DTDs must be resolved by URI
			config.add(new URIResolverPlugin());
			//Support formatting based on DTD restrictions
			config.add(new DTDCorePlugin());
			//Support formatting based on XSD restrictions
			config.add(new XSDCorePlugin());
			if (!allowExternalURI) {
				config.add(new PreventExternalURIResolverExtension());
			}
		}

		@Override
		public void customize() {
			//Register required EMF factories
			XSDSchemaBuildingTools.getXSDFactory();
			PluginPreferences.configure(XMLCorePlugin.getDefault(), new XMLCorePreferenceInitializer(), properties);
			PluginPreferences.configureCatalog(properties, (Catalog) XMLCorePlugin.getDefault().getDefaultXMLCatalog());
		}

	};

	/** Formatting XML string resolving URIs according its base location  */
	public String format(String raw, String baseLocation) throws Exception {
		IStructuredDocument document = new BasicStructuredDocument(new XMLSourceParser());
		document.setPreferredLineDelimiter(LINE_DELIMITER);
		IDocumentPartitioner partitioner = new StructuredTextPartitionerForXML();
		document.setDocumentPartitioner(new StructuredTextPartitionerForXML());
		partitioner.connect(document);
		document.set(raw);
		DOMModelImpl xmlDOM = new DOMModelImpl();
		xmlDOM.setBaseLocation(baseLocation);
		xmlDOM.getFactoryRegistry().addFactory(xmlAdapterFactory);
		xmlDOM.setStructuredDocument(document);
		ModelQuery modelQuery = ModelQueryUtil.getModelQuery(xmlDOM);
		modelQuery.getCMDocumentManager().setPropertyEnabled(CMDocumentManager.PROPERTY_USE_CACHED_RESOLVED_URI, true);
		TextEdit formatterChanges = formatter.format(xmlDOM, 0, document.getLength(), preferences);
		formatterChanges.apply(document);
		return document.get();
	}

}
