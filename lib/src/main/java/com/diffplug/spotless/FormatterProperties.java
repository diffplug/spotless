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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Utility manages settings of formatter configured by properties. */
public class FormatterProperties {

	private static final Logger LOG = Logger.getLogger(FormatterProperties.class.getName());

	private final Properties properties;

	/**
	 * Per default the property list is empty and the formatters default
	 * settings are used.
	 */
	public FormatterProperties() {
		properties = new Properties();
	}

	/**
	 * Import settings from a sequence of files (file import is the given order)
	 *
	 * @param settingsFiles
	 *            Sequence of files
	 * @throws IllegalArgumentException
	 *            In case the import of a file fails
	 */
	public void add(final Iterable<File> settingsFiles) throws IllegalArgumentException {
		settingsFiles.forEach(file -> add(file));
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
	public void add(final File settingsFile) throws IllegalArgumentException {
		if (!(settingsFile.isFile() || settingsFile.canRead())) {
			String msg = String.format("Settings file '%s' does not exist or can not be read.", settingsFile);
			throw new IllegalArgumentException(msg);
		}
		try {
			Properties newSettings = FileParser.parse(settingsFile);
			properties.putAll(newSettings);
		} catch (IOException | IllegalArgumentException | NullPointerException exception) {
			StringBuffer message = new StringBuffer(
					String.format("Failed to add properties from '%s' to formatter settings.", settingsFile));
			String detailedMessage = exception.getMessage();
			if (null != detailedMessage) {
				message.append(String.format(" %s", detailedMessage));
			}
			throw new IllegalArgumentException(message.toString(), exception);
		}
	}

	/**
	 * Get accumulated {@link java.util.Properties Properties}
	 * @return Properties
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Returns a {@link java.util.Set Set} view for the underlying property map.
	 *
	 * @return Property set
	 */
	public Set<Map.Entry<Object, Object>> entrySet() {
		return properties.entrySet();
	}

	private enum FileParser {
		LINE_ORIENTED("properties", "prefs") {
			@Override
			protected Properties execute(File file) throws IOException, IllegalArgumentException {
				Properties properties = new Properties();
				InputStream inputProperties = new FileInputStream(file);
				try {
					properties.load(inputProperties);
				} finally {
					inputProperties.close();
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
			final String fileName = file.getName();
			String fileNameExt = "";
			int seperatorPos = fileName.lastIndexOf(FILE_EXTENSION_SEPARATOR);
			if (0 < seperatorPos) {
				fileNameExt = fileName.substring(seperatorPos + 1);
			}
			return fileNameExt;
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
				Node firstProfile = getFirstProfile(file, rootNode);
				if (null != firstProfile) {
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
				}
				return properties;
			}

			@Nullable
			private Node getFirstProfile(File file, final Node rootNode) {
				List<Node> profiles = getChildren(rootNode, "profile");
				if (profiles.size() > 1) {
					String message = String.format("Formatter configuration file contains multiple profiles: '%s'%n    ",
							file.getAbsolutePath());
					message += profiles.stream().map(profile -> getProfileName(profile))
							.collect(Collectors.joining("; "));
					LOG.config(message);
				}
				Node firstProvile = null;
				if (profiles.size() == 0) {
					String msg = String.format(
							"Using formatter default configuration since formatter configuration file does not contain a profile: '%s'",
							file.getAbsolutePath());
					LOG.config(msg);
				} else {
					firstProvile = profiles.iterator().next();
					LOG.finest(String.format("Using profile '%s' from: '%s'", getProfileName(firstProvile), file.getAbsolutePath()));
				}
				return firstProvile;
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
