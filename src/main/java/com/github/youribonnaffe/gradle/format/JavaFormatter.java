package com.github.youribonnaffe.gradle.format;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.formatter.CodeFormatter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

/**
 * From Hibernate Tools
 */
public class JavaFormatter {

	private CodeFormatter codeFormatter;

	public JavaFormatter(Properties settings) {
		if (settings == null) {
			// if no settings run with jdk 5 as default
			settings = new Properties();
			settings.put(JavaCore.COMPILER_SOURCE, "1.5");
			settings.put(JavaCore.COMPILER_COMPLIANCE, "1.5");
			settings.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, "1.5");
		}
		this.codeFormatter = ToolFactory.createCodeFormatter(settings);
	}

	public boolean formatFile(File file) {
		IDocument doc = new Document();
		try {
			String contents = new String(org.eclipse.jdt.internal.compiler.util.Util.getFileCharContent(file, null));
			doc.set(contents);
			TextEdit edit = codeFormatter.format(CodeFormatter.K_COMPILATION_UNIT, contents, 0, contents.length(), 0,
					null);
			if (edit != null) {
				edit.apply(doc);
			} else {
				return false; // most likely syntax errror
			}

			// write the file
			final BufferedWriter out = new BufferedWriter(new FileWriter(file));
			try {
				out.write(doc.get());
				out.flush();
			} finally {
				try {
					out.close();
				} catch (IOException e) {
					/* ignore */
				}
			}
			return true;
		} catch (IOException e) {
			throw new RuntimeException("Could not format " + file, e);
		} catch (BadLocationException e) {
			throw new RuntimeException("Could not format " + file, e);
		}
	}

}
