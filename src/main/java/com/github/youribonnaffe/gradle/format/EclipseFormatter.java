package com.github.youribonnaffe.gradle.format;

import groovy.util.Node;
import groovy.util.NodeList;
import groovy.util.XmlParser;
import groovy.xml.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

/**
 * From Hibernate Tools
 */
public class EclipseFormatter extends FormatterStep {
	/** Returns an EclipseFormatter from the given config file. */
	public static EclipseFormatter readFrom(File file) throws Exception {
		Properties settings = new Properties();
		if (file == null) {
			settings.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_8);
			settings.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_8);
			settings.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_8);
			return new EclipseFormatter(settings);
		} else if (file.getName().endsWith(".properties")) {
			try (InputStream input = new FileInputStream(file)) {
				settings.load(input);
			}
			return new EclipseFormatter(settings);
		} else if (file.getName().endsWith(".xml")) {
			Node xmlSettings = new XmlParser().parse(file);
			NodeList xmlSettingsElements = xmlSettings.getAt(new QName("profile")).getAt("setting");
			for (int i = 0; i < xmlSettingsElements.size(); ++i) {
				Node setting = (Node) xmlSettingsElements.get(i);
				settings.put(setting.attributes().get("id"), setting.attributes().get("value"));
			}
			return new EclipseFormatter(settings);
		} else {
			throw new IllegalArgumentException("Eclipse formatter file must be .properties or .xml");
		}
	}

	private CodeFormatter codeFormatter;

	private EclipseFormatter(Properties settings) {
		if (settings == null) {
			// if no settings run with jdk 5 as default
			settings = new Properties();
		}
		this.codeFormatter = ToolFactory.createCodeFormatter(settings);
	}

	@Override
	public String format(String content) throws Exception {
		TextEdit edit = getEditForContent(content);
		if (edit == null) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.");
		} else {
			IDocument doc = new Document(content);
			edit.apply(doc);
			return doc.get();
		}
	}

	public TextEdit getEditForContent(String content) {
		return codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, content, 0, content.length(), 0,
				LineEnding.UNIX.string);
	}
}
