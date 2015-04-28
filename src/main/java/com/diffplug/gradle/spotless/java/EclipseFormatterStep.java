package com.diffplug.gradle.spotless.java;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

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

import com.diffplug.gradle.spotless.LineEnding;

/** Formatter step which calls out to the Eclipse formatter. */
public class EclipseFormatterStep {
	public static final String NAME = "Eclipse Formatter";

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
