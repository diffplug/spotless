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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin;
import org.eclipse.wst.dtd.core.internal.DTDCorePlugin;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.text.BasicStructuredDocument;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
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
import org.osgi.framework.BundleException;

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;
import com.diffplug.spotless.extra.eclipse.wtp.sse.SpotlessPreferences;

/** Formatter step which calls out to the Eclipse XML formatter. */
public class EclipseXmlFormatterStepImpl {
	/**
	 * Indicates if external URIs (location hints) should be resolved
	 * and the referenced DTD/XSD shall be applied. Per default
	 * external URIs are ignored.
	 * <p>
	 * Value is of type <code>Boolean</code>.
	 * </p>
	 */
	public static final String RESOLVE_EXTERNAL_URI = "resolveExternalURI";
	
	
	private static XmlFormattingPreferencesFactory PREFERENCE_FACTORY = null;

	private final DefaultXMLPartitionFormatter formatter;
	private final XMLFormattingPreferences preferences;
	private final INodeAdapterFactory xmlAdapterFactory;

	public EclipseXmlFormatterStepImpl(Properties properties) throws Exception {
		setupFramework(doResolveExternalURI(properties));
		preferences = PREFERENCE_FACTORY.create(properties);
		formatter = new DefaultXMLPartitionFormatter();
		//The adapter factory maintains the common CMDocumentCache
		xmlAdapterFactory = new ModelQueryAdapterFactoryForXML();
	}
	
	private static boolean doResolveExternalURI(Properties properties) {
		Object obj = properties.get(RESOLVE_EXTERNAL_URI);
		if(null != obj) {
			if(obj instanceof Boolean) {
				return (Boolean)obj;
			}
			if(obj instanceof String) {
				return ((String)obj).equalsIgnoreCase("true");
			}
		}
		return false;
	} 

	private static void setupFramework(boolean resolveExternalURI) throws BundleException {
		if (SpotlessEclipseFramework.setup(
				plugins -> {
					plugins.applyDefault();
					//The WST XML formatter
					plugins.add(new XMLCorePlugin());
					//XSDs/DTDs must be resolved by URI
					plugins.add(new URIResolverPlugin());
					//Support formatting based on DTD restrictions
					plugins.add(new DTDCorePlugin());
					//Support formatting based on XSD restrictions
					plugins.add(new XSDCorePlugin());
					if(!resolveExternalURI) {
						plugins.add(new PreventExternalURIResolverExtension());
					}
				})) {
			PREFERENCE_FACTORY = new XmlFormattingPreferencesFactory();
			//Register required EMF factories
			XSDSchemaBuildingTools.getXSDFactory();
		}
	}

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

	private static class XmlFormattingPreferencesFactory {
		private final static Set<String> SUPPORTED_XML_FORMAT_PREFS = new HashSet<String>(Arrays.asList(
				FORMAT_COMMENT_TEXT,
				FORMAT_COMMENT_JOIN_LINES,
				LINE_WIDTH,
				SPLIT_MULTI_ATTRS,
				ALIGN_END_BRACKET,
				SPACE_BEFORE_EMPTY_CLOSE_TAG,
				PRESERVE_CDATACONTENT,
				INDENTATION_CHAR,
				INDENTATION_SIZE,
				CLEAR_ALL_BLANK_LINES));

		XmlFormattingPreferencesFactory() {
			XMLCorePreferenceInitializer initializer = new XMLCorePreferenceInitializer();
			initializer.initializeDefaultPreferences();
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
			Properties properties = new Properties();
			properties.setProperty(CMDOCUMENT_GLOBAL_CACHE_ENABLED, Boolean.toString(false));
			Plugin plugin = XMLCorePlugin.getDefault();
			SpotlessPreferences.configurePluginPreferences(plugin, properties);
		}

		XMLFormattingPreferences create(final Properties properties) {
			SpotlessPreferences.configureCatalog(properties);
			return createFormattingPreference(properties);
		}

		private XMLFormattingPreferences createFormattingPreference(final Properties properties) {
			Properties newXmlProperties = getXMLFormattingProperties(properties);
			Plugin plugin = XMLCorePlugin.getDefault();
			Properties defaultXmlProperties = SpotlessPreferences.configurePluginPreferences(plugin, newXmlProperties);
			XMLFormattingPreferences xmlPreferences = new XMLFormattingPreferences();
			SpotlessPreferences.configurePluginPreferences(plugin, defaultXmlProperties);
			return xmlPreferences;
		}

		private Properties getXMLFormattingProperties(final Properties properties) {
			Properties filteredProperties = new Properties();
			properties.entrySet().stream().filter(
					entry -> SUPPORTED_XML_FORMAT_PREFS.contains(entry.getKey())).forEach(entry -> filteredProperties.put(entry.getKey(), entry.getValue()));
			return filteredProperties;
		}
	}

}
