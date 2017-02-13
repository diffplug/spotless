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
package com.diffplug.spotless.extra.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

/** Formatter step which calls out to the Eclipse formatter. */
public final class EclipseFormatterStep {
	// prevent direct instantiation
	private EclipseFormatterStep() {}

	private static final String DEFAULT_VERSION = "4.6.1";
	private static final String NAME = "eclipse formatter";
	private static final String MAVEN_COORDINATE = "com.diffplug.spotless:spotless-ext-eclipse-jdt:";
	private static final String FORMATTER_CLASS = "com.diffplug.gradle.spotless.java.eclipse.EclipseFormatterStepImpl";
	private static final String FORMATTER_METHOD = "format";

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(File settingsFile, Provisioner provisioner) {
		return create(defaultVersion(), settingsFile, provisioner);
	}

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(String version, File settingsFile, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME,
				() -> new State(JarState.from(MAVEN_COORDINATE + version, provisioner), settingsFile),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the eclipse formatter. */
		final JarState jarState;
		/** The signature of the settings file. */
		final FileSignature settings;

		State(JarState jar, File settingsFile) throws Exception {
			this.jarState = Objects.requireNonNull(jar);
			this.settings = FileSignature.from(
					Arrays.asList(settingsFile),
					FileSignature.Ignore.PREVIOUS_DUPLICATES);
		}

		FormatterFunc createFormat() throws Exception {
			Properties parsedSettings = parseProperties(settings.getOnlyFile());

			ClassLoader classLoader = jarState.getClassLoader();

			// instantiate the formatter and get its format method
			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(parsedSettings);
			Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
			return input -> (String) method.invoke(formatter, input);
		}
	}

	private static final Logger logger = Logger.getLogger(EclipseFormatterStep.class.getName());

	/** Parses an eclipse properties or XML file, determined dynamically based on the file ending. */
	private static Properties parseProperties(File file) throws Exception {
		Properties settings = new Properties();
		if (!file.exists()) {
			throw new IllegalArgumentException("Eclipse formatter file '" + file + "' does not exist.");
		} else if (file.getName().endsWith(".properties")) {
			try (InputStream input = new FileInputStream(file)) {
				settings.load(input);
			}
			return settings;
		} else if (file.getName().endsWith(".xml")) {
			Node xmlSettings = new XmlParser().parse(file);
			NodeList profiles = xmlSettings.getAt(new QName("profile"));
			if (profiles.size() > 1) {
				logger.warning("Eclipse formatter file contains multiple profiles: " + file.getAbsolutePath());
				for (Object profile : profiles) {
					Node node = (Node) profile;
					logger.warning("    " + node.attribute("name"));
				}
				logger.warning("Using first profile, recommend deleting others.");
			}
			NodeList xmlSettingsElements = xmlSettings.getAt(new QName("profile")).getAt("setting");
			for (Object xmlSettingsElement : xmlSettingsElements) {
				Node setting = (Node) xmlSettingsElement;
				settings.put(setting.attributes().get("id"), setting.attributes().get("value"));
			}
			return settings;
		} else {
			throw new IllegalArgumentException("Eclipse formatter file must be .properties or .xml");
		}
	}
}
