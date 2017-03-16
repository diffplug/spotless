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
package com.diffplug.spotless;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Utility manages settings of formatter configured by properties. */
public class FormatterProperties {

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
		FormatterProperties properties = new FormatterProperties();
		files.forEach(file -> properties.add(file));
		return properties;
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
		if (!(settingsFile.isFile() || settingsFile.canRead())) {
			String msg = String.format("Settings file '%s' does not exist or can not be read.", settingsFile);
			throw new IllegalArgumentException(msg);
		}
		try {
			Properties newSettings = FileParser.parse(settingsFile);
			properties.putAll(newSettings);
		} catch (IOException | IllegalArgumentException | NullPointerException exception) {
			String message = String.format("Failed to add properties from '%s' to formatter settings.", settingsFile);
			String detailedMessage = exception.getMessage();
			if (null != detailedMessage) {
				message += String.format(" %s", detailedMessage);
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
			protected Properties execute(File file) throws IOException, IllegalArgumentException {
				Properties properties = new Properties();
				try (InputStream inputProperties = new FileInputStream(file)) {
					properties.load(inputProperties);
				}
				return properties;
			}
		},

		XML("xml") {
			@Override
			protected Properties execute(final File file) throws IOException, IllegalArgumentException {
				Node rootNode = getRootNode(file);
				String nodeName = rootNode.getNodeName();
				if (null == nodeName) {
					throw new IllegalArgumentException("XML document does not contain a root node.");
				}
				return XmlParser.parse(file, rootNode);
			}

			private Node getRootNode(final File file) throws IOException, IllegalArgumentException {
				try {
					/*
					 * The parser does not validate, since the root node is only
					 * used to decide on further processing.
					 */
					DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					DocumentBuilder db = dbf.newDocumentBuilder();
					return db.parse(file).getDocumentElement();
				} catch (SAXException | ParserConfigurationException e) {
					throw new IllegalArgumentException("File has no valid XML syntax.", e);
				}

			}

		};
		private static final char FILE_EXTENSION_SEPARATOR = '.';

		private final List<String> supportedFileNameExtensions;

		private FileParser(final String... supportedFileNameExtensions) {
			this.supportedFileNameExtensions = Arrays.asList(supportedFileNameExtensions);
		}

		protected abstract Properties execute(File file) throws IOException, IllegalArgumentException;

		public static Properties parse(final File file) throws IOException, IllegalArgumentException {
			String fileNameExtension = getFileNameExtension(file);
			for (FileParser parser : FileParser.values()) {
				if (parser.supportedFileNameExtensions.contains(fileNameExtension)) {
					return parser.execute(file);
				}
			}
			String msg = String.format(
					"The file name extension '%1$s' is not part of the supported file extensions [%2$s].",
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
			protected Properties execute(final File xmlFile, final Node rootNode)
					throws IOException, IllegalArgumentException {
				final Properties properties = new Properties();
				InputStream xmlInput = new FileInputStream(xmlFile);
				try {
					properties.loadFromXML(xmlInput);
				} finally {
					xmlInput.close();
				}
				return properties;
			}
		},

		PROFILES("profiles") {
			@Override
			protected Properties execute(File file, Node rootNode) throws IOException, IllegalArgumentException {
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
				if (profiles.size() == 0) {
					throw new IllegalArgumentException("The formatter configuration profile files does not contain any 'profile' elements.");
				}
				if (profiles.size() > 1) {
					String message = "Formatter configuration file contains multiple profiles: [";
					message += profiles.stream().map(profile -> getProfileName(profile))
							.collect(Collectors.joining("; "));
					message += "]%n The formatter can only cope with a single profile per configuration file. Please remove the other profiles.";
					throw new IllegalArgumentException(message);
				}
				return profiles.iterator().next();
			}

			private List<Node> getChildren(final Node node, final String nodeName) {
				List<Node> matchingChildren = new LinkedList<Node>();
				NodeList children = node.getChildNodes();
				for (int i = 0; i < children.getLength(); i++) {
					Node child = children.item(i);
					if (child.getNodeName().equals(nodeName)) {
						matchingChildren.add(child);
					}
				}
				return matchingChildren;
			}

		};

		private static String getProfileName(Node profile) {
			Node nameAttribute = profile.getAttributes().getNamedItem("name");
			return (null == nameAttribute) ? "" : nameAttribute.getNodeValue();
		}

		private final String rootNodeName;

		private XmlParser(final String rootNodeName) {
			this.rootNodeName = rootNodeName;
		}

		@Override
		public String toString() {
			return this.rootNodeName;
		}

		protected abstract Properties execute(File file, Node rootNode) throws IOException, IllegalArgumentException;

		public static Properties parse(final File file, final Node rootNode)
				throws IOException, IllegalArgumentException {
			String rootNodeName = rootNode.getNodeName();
			for (XmlParser parser : XmlParser.values()) {
				if (parser.rootNodeName.equals(rootNodeName)) {
					return parser.execute(file, rootNode);
				}
			}
			String msg = String.format("The XML root node '%1$s' is not part of the supported root nodes [%2$s].",
					rootNodeName, Arrays.toString(XmlParser.values()));
			throw new IllegalArgumentException(msg);
		}

	}

}
