package com.github.youribonnaffe.gradle.format;

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
public class EclipseFormatter {

	/** Enforces line endings using the Eclipse formatter. */
	public enum LineEnding {
		PLATFORM_NATIVE(null),
		WINDOWS("\r\n"),
		UNIX("\n");

		private final String string;

		private LineEnding(String ending) {
			this.string = ending;
		}
	}

	private CodeFormatter codeFormatter;
	private LineEnding lineEnding;

	public EclipseFormatter(Properties settings, LineEnding lineEnding) {
		if (settings == null) {
			// if no settings run with jdk 5 as default
			settings = new Properties();
			settings.put(JavaCore.COMPILER_SOURCE, "1.5");
			settings.put(JavaCore.COMPILER_COMPLIANCE, "1.5");
			settings.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.5");
		}
		this.codeFormatter = ToolFactory.createCodeFormatter(settings);
		this.lineEnding = lineEnding;
	}

	public boolean editRequired(String content) throws Exception {
		String formatted = format(content);
		return formatted.equals(content);
	}

	public String format(String content) throws Exception {
		TextEdit edit = getEditForContent(content);
		if (edit == null) {
			throw new IllegalArgumentException("Invalid java syntax for formatting.");
		} else {
			IDocument doc = new Document();
			doc.set(content);
			edit.apply(doc);
			return doc.get();
		}
	}

	public TextEdit getEditForContent(String content) {
		return codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, content, 0, content.length(), 0, lineEnding.string);
	}
}
