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

import java.util.Properties;

import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.text.BasicStructuredDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.document.DOMModelImpl;
import org.eclipse.wst.xml.core.internal.formatter.DefaultXMLPartitionFormatter;
import org.eclipse.wst.xml.core.internal.formatter.XMLFormattingPreferences;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryAdapterFactoryForXML;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.parser.XMLSourceParser;
import org.eclipse.wst.xml.core.internal.text.rules.StructuredTextPartitionerForXML;

/** Formatter step which calls out to the Eclipse formatter. */
public class EclipseXmlFormatterStepImpl {
	/** Spotless always uses \n internally as line delimiter */
	public static final String LINE_DELIMITER = "\n";

	private final DefaultXMLPartitionFormatter formatter;
	private final XMLFormattingPreferences preferences;
	private final INodeAdapterFactory xmlAdapterFactory;

	public EclipseXmlFormatterStepImpl(Properties properties) throws Exception {
		preferences = EclipseXmlFormatterFramework.setup(properties);
		formatter = new DefaultXMLPartitionFormatter();
		//The adapter factory maintains the common CMDocumentCache
		xmlAdapterFactory = new ModelQueryAdapterFactoryForXML();
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

}
