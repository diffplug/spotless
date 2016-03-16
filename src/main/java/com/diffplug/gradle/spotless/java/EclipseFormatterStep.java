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
import java.util.Properties;

import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import com.diffplug.gradle.spotless.LineEnding;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

/** Formatter step which calls out to the Eclipse formatter. */
public class EclipseFormatterStep {
	public static final String NAME = "Eclipse Formatter";
	private static final Logger logger = Logging.getLogger(EclipseFormatterStep.class);

	private CodeFormatter codeFormatter;

	private EclipseFormatterStep(Properties settings) {
		this.codeFormatter = ToolFactory.createCodeFormatter(settings);
	}

	public String format(String raw) throws Exception {
		TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, raw, 0, raw.length(), 0, LineEnding.UNIX.string);
		if (edit == null) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.");
		} else {
			IDocument doc = new Document(raw);
			edit.apply(doc);
			return doc.get();
		}
	}

	/** Returns an EclipseFormatterStep from the given config file. */
	public static EclipseFormatterStep load(File file) throws Exception {
		Properties settings = new Properties();
		if (!file.exists()) {
			throw new GradleException("Eclipse formatter file '" + file + "' does not exist.");
		} else if (file.getName().endsWith(".properties")) {
			try (InputStream input = new FileInputStream(file)) {
				settings.load(input);
			}
			return new EclipseFormatterStep(settings);
		} else if (file.getName().endsWith(".xml")) {
			Node xmlSettings = new XmlParser().parse(file);
			NodeList profiles = xmlSettings.getAt(new QName("profile"));
			if (profiles.size() > 1) {
				logger.warn("Eclipse formatter file contains multiple profiles: " + file.getAbsolutePath());
				for (int i = 0; i < profiles.size(); ++i) {
					Node node = (Node) profiles.get(i);
					logger.warn("    " + node.attribute("name"));
				}
				logger.warn("Using first profile, recommend deleting others.");
			}
			NodeList xmlSettingsElements = xmlSettings.getAt(new QName("profile")).getAt("setting");
			for (int i = 0; i < xmlSettingsElements.size(); ++i) {
				Node setting = (Node) xmlSettingsElements.get(i);
				settings.put(setting.attributes().get("id"), setting.attributes().get("value"));
			}
			return new EclipseFormatterStep(settings);
		} else {
			throw new GradleException("Eclipse formatter file must be .properties or .xml");
		}
	}
}
