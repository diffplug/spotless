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
package com.diffplug.gradle.spotless.java;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Properties;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.diffplug.common.base.Throwing;
import com.diffplug.gradle.spotless.FileSignature;
import com.diffplug.gradle.spotless.JarState;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

/** Formatter step which calls out to the Eclipse formatter. */
class EclipseFormatterStep implements Serializable {
	private static final long serialVersionUID = 1L;

	// static final String DEFAULT_VERSION = "4.6.1";
	static final String DEFAULT_VERSION = "4.6.1-SNAPSHOT";
	static final String MAVEN_COORDINATE = "com.diffplug.gradle.spotless:spotless-eclipse:";
	static final String FORMATTER_CLASS = "com.diffplug.gradle.spotless.java.eclipse.EclipseFormatterStepImpl";
	static final String FORMATTER_METHOD = "format";

	/** The jar that contains the eclipse formatter. */
	final JarState jarState;
	/** The signature of the jar. */
	final FileSignature settings;

	EclipseFormatterStep(Project project, String version, Object settingsFile) throws Exception {
		this.jarState = new JarState(MAVEN_COORDINATE + version, project);
		this.settings = new FileSignature(project.file(settingsFile));
	}

	static final String NAME = "eclipse formatter";

	private static final Logger logger = Logging.getLogger(EclipseFormatterStep.class);

	public Throwing.Function<String, String> createFormat() throws Exception {
		Properties parsedSettings = parseProperties(settings.getOnlyFile());

		URLClassLoader classLoader = jarState.openIsolatedClassLoader();
		// TODO: dispose the classloader when the function
		// that we return gets garbage-collected

		// instantiate the gjf formatter and get its format method
		Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
		Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(parsedSettings);
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
		return input -> (String) method.invoke(formatter, input);
	}

	static Properties parseProperties(File file) throws Exception {
		Properties settings = new Properties();
		if (!file.exists()) {
			throw new GradleException("Eclipse formatter file '" + file + "' does not exist.");
		} else if (file.getName().endsWith(".properties")) {
			try (InputStream input = new FileInputStream(file)) {
				settings.load(input);
			}
			return settings;
		} else if (file.getName().endsWith(".xml")) {
			Node xmlSettings = new XmlParser().parse(file);
			NodeList profiles = xmlSettings.getAt(new QName("profile"));
			if (profiles.size() > 1) {
				logger.warn("Eclipse formatter file contains multiple profiles: " + file.getAbsolutePath());
				for (Object profile : profiles) {
					Node node = (Node) profile;
					logger.warn("    " + node.attribute("name"));
				}
				logger.warn("Using first profile, recommend deleting others.");
			}
			NodeList xmlSettingsElements = xmlSettings.getAt(new QName("profile")).getAt("setting");
			for (Object xmlSettingsElement : xmlSettingsElements) {
				Node setting = (Node) xmlSettingsElement;
				settings.put(setting.attributes().get("id"), setting.attributes().get("value"));
			}
			return settings;
		} else {
			throw new GradleException("Eclipse formatter file must be .properties or .xml");
		}
	}
}
