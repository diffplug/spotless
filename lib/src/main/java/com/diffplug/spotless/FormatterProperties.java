/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless;

import static com.diffplug.spotless.MoreIterables.toNullHostileList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Utility manages settings of formatter configured by properties. */
public final class FormatterProperties {

	private final Properties properties;

	private FormatterProperties() {
		properties = new Properties();
	}

	/**
	 * Import settings from a sequence of files (file import is the given order)
	 *
	 * @param files
	 *            Sequence of files
	 * @throws IllegalArgumentException
	 *            In case the import of a file fails
	 */
	public static FormatterProperties from(File... files) throws IllegalArgumentException {
		Objects.requireNonNull(files);
		return from(Arrays.asList(files));
	}

	/**
	 * Import settings from a sequence of files (file import is the given order)
	 *
	 * @param files
	 *            Sequence of files
	 * @throws IllegalArgumentException
	 *            In case the import of a file fails
	 */
	public static FormatterProperties from(Iterable<File> files) throws IllegalArgumentException {
		List<File> nonNullFiles = toNullHostileList(files);
		FormatterProperties properties = new FormatterProperties();
		nonNullFiles.forEach(properties::add);
		return properties;
	}

	public static FormatterProperties fromPropertiesContent(Iterable<String> content) throws IllegalArgumentException {
		List<String> nonNullElements = toNullHostileList(content);
		FormatterProperties properties = new FormatterProperties();
		nonNullElements.forEach(contentElement -> {
			try (InputStream is = new ByteArrayInputStream(contentElement.getBytes(StandardCharsets.UTF_8))) {
				properties.properties.load(is);
			} catch (IOException e) {
				throw new IllegalArgumentException("Unable to load properties: " + contentElement);
			}
		});
		return properties;
	}

	public static FormatterProperties fromXmlContent(final Iterable<String> content) throws IllegalArgumentException {
		final List<String> nonNullElements = toNullHostileList(content);
		final FormatterProperties properties = new FormatterProperties();
		nonNullElements.forEach(contentElement -> {
			try {
				final Properties newSettings = FileParser.XML.executeXmlContent(contentElement);
				properties.properties.putAll(newSettings);
			} catch (IOException | IllegalArgumentException exception) {
				String message = "Failed to add preferences from XML:%n%s%n".formatted(contentElement);
				final String detailedMessage = exception.getMessage();
				if (null != detailedMessage) {
					message += " %s".formatted(detailedMessage);
				}
				throw new IllegalArgumentException(message, exception);
			}
		});
		return properties;
	}

	public static FormatterProperties merge(Properties... properties) {
		FormatterProperties merged = new FormatterProperties();
		List.of(properties).stream().forEach((source) -> merged.properties.putAll(source));
		return merged;
	}

	/**
	 * Import settings from given file. New settings (with the same ID/key)
	 * override existing once.
	 *
	 * @param settingsFile
	 *            File
	 * @throws IllegalArgumentException
	 *            In case the import of the file fails
	 */
	private void add(final File settingsFile) throws IllegalArgumentException {
		Objects.requireNonNull(settingsFile);
		if (!(settingsFile.isFile() && settingsFile.canRead())) {
			String msg = "Settings file '%s' does not exist or can not be read.".formatted(settingsFile);
			throw new IllegalArgumentException(msg);
		}
		try {
			Properties newSettings = FileParser.parse(settingsFile);
			properties.putAll(newSettings);
		} catch (IOException | IllegalArgumentException exception) {
			String message = "Failed to add properties from '%s' to formatter settings.".formatted(settingsFile);
			String detailedMessage = exception.getMessage();
			if (null != detailedMessage) {
				message += " %s".formatted(detailedMessage);
			}
			throw new IllegalArgumentException(message, exception);
		}
	}

	/** Returns the accumulated {@link java.util.Properties Properties} */
	public Properties getProperties() {
		return properties;
	}

	private enum FileParser {
		LINE_ORIENTED("properties", "prefs") {
			@Override
			protected Properties execute(final File file) throws IOException, IllegalArgumentException {
				Properties properties = new Properties();
				try (InputStream inputProperties = new FileInputStream(file)) {
					properties.load(inputProperties);
				}
				return properties;
			}

			@Override
			protected Properties executeXmlContent(String content) throws IOException, IllegalArgumentException {
				throw new RuntimeException("Not implemented");
			}
		},

		XML("xml") {
			@Override
			protected Properties execute(final File file) throws IOException, IllegalArgumentException {
				return executeWithSupplier(() -> {
					try {
						return new FileInputStream(file);
					} catch (FileNotFoundException e) {
						throw new RuntimeException("File not found: " + file, e);
					}
				});
			}

			@Override
			protected Properties executeXmlContent(String content) throws IOException, IllegalArgumentException {
				return executeWithSupplier(() -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
			}

			private Properties executeWithSupplier(Supplier<InputStream> isSupplier) throws IOException, IllegalArgumentException {
				Node rootNode;
				try (InputStream input = isSupplier.get()) {
					rootNode = getRootNode(input);
					String nodeName = rootNode.getNodeName();
					if (null == nodeName) {
						throw new IllegalArgumentException("XML document does not contain a root node.");
					}
				}
				try (InputStream input = isSupplier.get()) {
					return XmlParser.parse(input, rootNode);
				}
			}

			private Node getRootNode(final InputStream is) throws IOException, IllegalArgumentException {
				try {
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					/*
					 * It is not required to validate or normalize attribute values for
					 * the XMLs currently supported. Disabling validation is supported by
					 * JavaX XML, but disabling normalization of attributes is not,
					 * since it contradicts the usage of XML.
					 * Here we work-around the attempt to load the properties.dtd.
					 * With Java 9, this work-around can be replaced by the usage of the XML
					 * catalog and provision of the SUN preperties.dtd.
					 */
					dbf.setFeature(LOAD_EXTERNAL_DTD_PROP, false);
					DocumentBuilder db = dbf.newDocumentBuilder();
					return db.parse(is).getDocumentElement();
				} catch (SAXException | ParserConfigurationException e) {
					throw new IllegalArgumentException("File has no valid XML syntax.", e);
				}

			}

		};

		private static final String LOAD_EXTERNAL_DTD_PROP = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
		private static final char FILE_EXTENSION_SEPARATOR = '.';

		private final List<String> supportedFileNameExtensions;

		FileParser(final String... supportedFileNameExtensions) {
			this.supportedFileNameExtensions = Arrays.asList(supportedFileNameExtensions);
		}

		protected abstract Properties execute(File file) throws IOException, IllegalArgumentException;

		protected abstract Properties executeXmlContent(String content) throws IOException, IllegalArgumentException;

		public static Properties parse(final File file) throws IOException, IllegalArgumentException {
			String fileNameExtension = getFileNameExtension(file);
			for (FileParser parser : FileParser.values()) {
				if (parser.supportedFileNameExtensions.contains(fileNameExtension)) {
					return parser.execute(file);
				}
			}
			String msg = "The file name extension '%1$s' is not part of the supported file extensions [%2$s].".formatted(
					fileNameExtension, Arrays.toString(FileParser.values()));
			throw new IllegalArgumentException(msg);

		}

		private static String getFileNameExtension(File file) {
			String fileName = file.getName();
			int seperatorPos = fileName.lastIndexOf(FILE_EXTENSION_SEPARATOR);
			return 0 > seperatorPos ? "" : fileName.substring(seperatorPos + 1);
		}

	}

	private enum XmlParser {
		PROPERTIES("properties") {
			@Override
			protected Properties execute(final InputStream xmlFile, final Node rootNode)
					throws IOException, IllegalArgumentException {
				final Properties properties = new Properties();
				properties.loadFromXML(xmlFile);
				return properties;
			}
		},

		PROFILES("profiles") {
			@Override
			protected Properties execute(InputStream file, Node rootNode) throws IOException, IllegalArgumentException {
				final Properties properties = new Properties();
				Node firstProfile = getSingleProfile(rootNode);
				for (Object settingObj : getChildren(firstProfile, "setting")) {
					Node setting = (Node) settingObj;
					NamedNodeMap attributes = setting.getAttributes();
					Node id = attributes.getNamedItem("id");
					Node value = attributes.getNamedItem("value");
					if (null == id) {
						throw new IllegalArgumentException("Node 'setting' does not possess an 'id' attribute.");
					}
					String idString = id.getNodeValue();
					/*
					 * A missing value is interpreted as an empty string,
					 * similar to the Properties behavior
					 */
					String valString = (null == value) ? "" : value.getNodeValue();
					properties.setProperty(idString, valString);
				}
				return properties;
			}

			private Node getSingleProfile(final Node rootNode) throws IllegalArgumentException {
				List<Node> profiles = getChildren(rootNode, "profile");
				if (profiles.isEmpty()) {
					throw new IllegalArgumentException("The formatter configuration profile files does not contain any 'profile' elements.");
				}
				if (profiles.size() > 1) {
					String message = "Formatter configuration file contains multiple profiles: [";
					message += profiles.stream().map(XmlParser::getProfileName).collect(Collectors.joining("; "));
					message += "]%n The formatter can only cope with a single profile per configuration file. Please remove the other profiles.";
					throw new IllegalArgumentException(message);
				}
				return profiles.getFirst();
			}

			private List<Node> getChildren(final Node node, final String nodeName) {
				NodeList children = node.getChildNodes();
				return IntStream.range(0, children.getLength())
						.mapToObj(children::item)
						.filter(child -> child.getNodeName().equals(nodeName))
						.collect(Collectors.toCollection(LinkedList::new));
			}

		};

		private static String getProfileName(Node profile) {
			Node nameAttribute = profile.getAttributes().getNamedItem("name");
			return (null == nameAttribute) ? "" : nameAttribute.getNodeValue();
		}

		private final String rootNodeName;

		XmlParser(final String rootNodeName) {
			this.rootNodeName = rootNodeName;
		}

		@Override
		public String toString() {
			return this.rootNodeName;
		}

		protected abstract Properties execute(InputStream is, Node rootNode) throws IOException, IllegalArgumentException;

		public static Properties parse(final InputStream is, final Node rootNode)
				throws IOException, IllegalArgumentException {
			String rootNodeName = rootNode.getNodeName();
			for (XmlParser parser : XmlParser.values()) {
				if (parser.rootNodeName.equals(rootNodeName)) {
					return parser.execute(is, rootNode);
				}
			}
			String msg = "The XML root node '%1$s' is not part of the supported root nodes [%2$s].".formatted(
					rootNodeName, Arrays.toString(XmlParser.values()));
			throw new IllegalArgumentException(msg);
		}

	}

}
