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
package com.diffplug.spotless.extra.glue.jdt;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPathExpressionException;

import org.assertj.core.util.Sets;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.internal.corext.fix.CleanUpConstantsOptions;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/** Provides configured clean-up implementations. */
final class CleanUpFactory {

	private final static Set<String> UNSUPPORTED_CLASSES = Collections.unmodifiableSet(Sets.newLinkedHashSet(
			"org.eclipse.jdt.internal.ui.fix.UnimplementedCodeCleanUp" //Would require Eclipse templates
	));

	@SuppressWarnings("serial")
	private final static Map<String, FixedValue> UNSUPPORTED_CONFIG = Collections.unmodifiableMap(new HashMap<String, FixedValue>() {
		{
			put(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS, new FixedValue("false", "Unused import clean-up only works in case all imports can be resolved. As an alternative use: " + CleanUpConstants.ORGANIZE_IMPORTS));
		}
	});

	private final static String CLEAN_UP_CONFIG_FILE_NAME = "plugin.xml";
	private final static String CLEAN_UP_CONFIG_DEPENDENCY_NAME = "org.eclipse.jdt.ui";
	private static List<Constructor<? extends ICleanUp>> CLEAN_UP_SEQUENCE = null;
	private final CleanUpOptions options;

	CleanUpFactory(Properties settings) {
		options = new CleanUpOptions();
		Logger logger = LoggerFactory.getLogger(CleanUpFactory.class);
		CleanUpConstantsOptions.setDefaultOptions(CleanUpConstants.DEFAULT_CLEAN_UP_OPTIONS, options);
		UNSUPPORTED_CONFIG.entrySet().stream().forEach(entry -> options.setOption(entry.getKey(), entry.getValue().value));
		settings.forEach((key, value) -> {
			FixedValue fixed = UNSUPPORTED_CONFIG.get(key);
			if (null != fixed && fixed.value != value) {
				logger.warn(String.format("Using %s for %s instead of %s: %s", fixed.value, key, value, fixed.reason));
			} else {
				options.setOption(key.toString(), value.toString());
			}
		});
		try {
			initializeCleanupActions();
		} catch (IOException | ParserConfigurationException | XPathExpressionException e) {
			throw new RuntimeException("Faild to read Eclipse Clean-Up configuration.", e);
		}
	}

	private static synchronized void initializeCleanupActions() throws IOException, ParserConfigurationException, XPathExpressionException {
		if (null != CLEAN_UP_SEQUENCE) {
			return;
		}
		ClassLoader loader = CleanUpFactory.class.getClassLoader();
		Optional<URL> configUrl = Collections.list(loader.getResources(CLEAN_UP_CONFIG_FILE_NAME)).stream().filter(url -> url.getPath().contains(CLEAN_UP_CONFIG_DEPENDENCY_NAME)).findAny();
		if (!configUrl.isPresent()) {
			throw new RuntimeException("Could not find JAR containing " + CLEAN_UP_CONFIG_DEPENDENCY_NAME + ":" + CLEAN_UP_CONFIG_FILE_NAME);
		}
		InputStream configXmlStream = configUrl.get().openStream();
		try {
			SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
			CleanUpExtensionHandler handler = new CleanUpExtensionHandler();
			saxParser.parse(configXmlStream, handler);
			CLEAN_UP_SEQUENCE = handler.getCleanUpSequence();
		} catch (SAXException e) {
			//Add information about the XML location
			throw new RuntimeException("Failed to parse " + configUrl.get().toExternalForm(), e);
		}
	}

	public List<ICleanUp> create() {
		return CLEAN_UP_SEQUENCE.stream().map(constructor -> {
			try {
				ICleanUp cleanUp = constructor.newInstance();
				cleanUp.setOptions(options);
				return cleanUp;
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Failed to created clean-up action for " + constructor.getName(), e);
			}
		}).collect(Collectors.toList());
	}

	private static class FixedValue {
		public final String value;
		public final String reason;

		FixedValue(String value, String reason) {
			this.value = value;
			this.reason = reason;
		}
	};

	private final static class CleanUpExtensionHandler extends DefaultHandler {
		private final static String CLEAN_UP_ELEMENT_NAME = "cleanUp";
		private final static String ID_ATTRIBUTE_NAME = "id";
		private final static String CLASS_ATTRIBUTE_NAME = "class";
		private final static String RUN_AFTER_ATTRIBUTE_NAME = "runAfter";
		private final Map<String, Constructor<? extends ICleanUp>> constructor;
		private final Map<String, String> runAfter;
		private final LinkedList<String> sorted;

		CleanUpExtensionHandler() {
			constructor = new HashMap<>();
			runAfter = new LinkedHashMap<>(); //E.g. the elements are already sorted
			sorted = new LinkedList<>();
		}

		@Override
		public void startDocument() throws SAXException {
			constructor.clear();
			runAfter.clear();
			sorted.clear();
			super.startDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (CLEAN_UP_ELEMENT_NAME == qName) {
				String id = getMandatoryAttribute(attributes, ID_ATTRIBUTE_NAME);
				String className = getMandatoryAttribute(attributes, CLASS_ATTRIBUTE_NAME);
				if (!UNSUPPORTED_CLASSES.contains(className)) {
					try {
						Class<?> clazz = Class.forName(className);
						Class<? extends ICleanUp> clazzImplementsICleanUp = clazz.asSubclass(ICleanUp.class);
						constructor.put(id, clazzImplementsICleanUp.getConstructor());
					} catch (ClassNotFoundException | ClassCastException | NoSuchMethodException | SecurityException e) {
						throw new SAXException("Failed to obtain constructor for " + CLEAN_UP_ELEMENT_NAME + " element class " + className, e);
					}
				}
				String runAfterId = attributes.getValue(RUN_AFTER_ATTRIBUTE_NAME);
				if (null == runAfterId) {
					sorted.push(id);
				} else {
					runAfter.put(id, runAfterId);
				}
			}
			super.startElement(uri, localName, qName, attributes);
		}

		private static String getMandatoryAttribute(Attributes attributes, String qName) throws SAXException {
			String value = attributes.getValue(qName);
			if (null == value) {
				throw new SAXException(CLEAN_UP_ELEMENT_NAME + " element without " + qName + " attribute.");
			}
			return value;
		}

		@Override
		public void endDocument() throws SAXException {
			if (runAfter.isEmpty()) {
				throw new SAXException(CLEAN_UP_ELEMENT_NAME + " element has not been found in XML.");
			}
			while (!runAfter.isEmpty()) {
				//E.g. the elements are already sorted. Hence only one iteration is expected.
				List<String> foundEntries = new ArrayList<>(runAfter.size());
				for (Map.Entry<String, String> entry : runAfter.entrySet()) {
					int runAfterIndex = sorted.lastIndexOf(entry.getValue());
					if (0 <= runAfterIndex) {
						foundEntries.add(entry.getKey());
						sorted.add(runAfterIndex + 1, entry.getKey());
					}
				}
				foundEntries.forEach(e -> runAfter.remove(e));
				if (foundEntries.isEmpty()) {
					throw new SAXException(CLEAN_UP_ELEMENT_NAME + " element the following precessor IDs cannot be resolved: " + runAfter.values().stream().collect(Collectors.joining("; ")));
				}
			}
			super.endDocument();
		}

		public List<Constructor<? extends ICleanUp>> getCleanUpSequence() {
			return sorted.stream().map(id -> constructor.get(id)).filter(clazz -> null != clazz).collect(Collectors.toList());
		}
	}

}
