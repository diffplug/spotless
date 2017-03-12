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
package com.diffplug.gradle.spotless.groovy.eclipse;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.codehaus.groovy.eclipse.core.GroovyCoreActivator;
import org.codehaus.groovy.eclipse.refactoring.formatter.DefaultGroovyFormatter;
import org.codehaus.groovy.eclipse.refactoring.formatter.FormatterPreferencesOnStore;
import org.codehaus.groovy.eclipse.refactoring.formatter.GroovyFormatter;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.text.edits.TextEdit;

/** Spotless-Formatter step which calls out to the Groovy-Eclipse formatter. */
public class GrEclipseFormatterStepImpl {
	private final FormatterPreferencesOnStore preferencesStore;

	public GrEclipseFormatterStepImpl(final Properties properties) throws Exception {
		PreferenceStore preferences = createPreferences(properties);
		preferencesStore = new FormatterPreferencesOnStore(preferences);
	}

	public String format(String raw) throws Exception {
		IDocument doc = new Document(raw);
		GroovyErrorListener errorListener = new GroovyErrorListener();
		TextSelection selectAll = new TextSelection(doc, 0, doc.getLength());
		GroovyFormatter codeFormatter = new DefaultGroovyFormatter(selectAll, doc, preferencesStore, false);
		TextEdit edit = codeFormatter.format();
		if (errorListener.errorsDetected()) {
			throw new IllegalArgumentException(errorListener.toString());
		}
		edit.apply(doc);
		return doc.get();
	}

	private static class GroovyErrorListener implements ILogListener {

		private final List<String> errors;

		public GroovyErrorListener() {
			errors = new LinkedList<String>();
			ILog groovyLogger = GroovyCoreActivator.getDefault().getLog();
			groovyLogger.addLogListener(this);
		}

		@Override
		public void logging(final IStatus status, final String plugin) {
			if (!status.isOK()) {
				errors.add(status.getMessage());
			}
		}

		public boolean errorsDetected() {
			ILog groovyLogger = GroovyCoreActivator.getDefault().getLog();
			groovyLogger.removeLogListener(this);
			return 0 != errors.size();
		}

		@Override
		public String toString() {
			StringBuilder string = new StringBuilder();
			if (1 < errors.size()) {
				string.append("Multiple problems detected during step execution:");
			} else if (0 == errors.size()) {
				string.append("Step sucesfully executed.");
			}
			for (String error : errors) {
				string.append(System.lineSeparator());
				string.append(error);
			}

			return string.toString();
		}

	}

	private static PreferenceStore createPreferences(final Properties properties) throws IOException {
		final PreferenceStore preferences = new PreferenceStore();
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		properties.store(output, null);
		ByteArrayInputStream input = new ByteArrayInputStream(output.toByteArray());
		preferences.load(input);
		return preferences;
	}

}
