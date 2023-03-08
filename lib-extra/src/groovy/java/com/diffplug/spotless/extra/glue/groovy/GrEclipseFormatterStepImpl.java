/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.extra.glue.groovy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.codehaus.groovy.eclipse.GroovyLogManager;
import org.codehaus.groovy.eclipse.IGroovyLogger;
import org.codehaus.groovy.eclipse.TraceCategory;
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

import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseFramework;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipsePluginConfig;
import com.diffplug.spotless.extra.eclipse.base.SpotlessEclipseServiceConfig;

/** Spotless-Formatter step which calls out to the Groovy-Eclipse formatter. */
public class GrEclipseFormatterStepImpl {
	/**
	 * Groovy compiler problems can be ignored.
	 * <p>
	 * Value is either 'true' or 'false'
	 * </p>
	 */
	public static final String IGNORE_FORMATTER_PROBLEMS = "ignoreFormatterProblems";

	private final FormatterPreferencesOnStore preferencesStore;
	private final boolean ignoreFormatterProblems;

	public GrEclipseFormatterStepImpl(final Properties properties) throws Exception {
		SpotlessEclipseFramework.setup(new FrameworkConfig());
		PreferenceStore preferences = createPreferences(properties);
		preferencesStore = new FormatterPreferencesOnStore(preferences);
		ignoreFormatterProblems = Boolean.parseBoolean(properties.getProperty(IGNORE_FORMATTER_PROBLEMS, "false"));
	}

	private static class FrameworkConfig implements SpotlessEclipseConfig {
		@Override
		public void registerServices(SpotlessEclipseServiceConfig config) {
			config.applyDefault();
			config.useSlf4J(GrEclipseFormatterStepImpl.class.getPackage().getName());
		}

		@Override
		public void activatePlugins(SpotlessEclipsePluginConfig config) {
			config.add(new GroovyCoreActivator());
		}
	}

	/** Formatting Groovy string  */
	public String format(String raw) throws Exception {
		IDocument doc = new Document(raw);
		GroovyErrorListener errorListener = new GroovyErrorListener();
		TextSelection selectAll = new TextSelection(doc, 0, doc.getLength());
		GroovyFormatter codeFormatter = new DefaultGroovyFormatter(selectAll, doc, preferencesStore, false);
		TextEdit edit = codeFormatter.format();
		if (!ignoreFormatterProblems && errorListener.errorsDetected()) {
			throw new IllegalArgumentException(errorListener.toString());
		}
		edit.apply(doc);
		return doc.get();
	}

	/**
	 * Eclipse Groovy formatter does not signal problems by its return value, but by logging errors.
	 */
	private static class GroovyErrorListener implements ILogListener, IGroovyLogger {

		private final List<String> errors;

		public GroovyErrorListener() {
			/*
			 * We need a synchronized list here, in case multiple instantiations
			 * run in parallel.
			 */
			errors = Collections.synchronizedList(new ArrayList<String>());
			ILog groovyLogger = GroovyCoreActivator.getDefault().getLog();
			groovyLogger.addLogListener(this);
			synchronized (GroovyLogManager.manager) {
				GroovyLogManager.manager.addLogger(this);
			}
		}

		@Override
		public void logging(final IStatus status, final String plugin) {
			errors.add(status.getMessage());
		}

		public boolean errorsDetected() {
			ILog groovyLogger = GroovyCoreActivator.getDefault().getLog();
			groovyLogger.removeLogListener(this);
			synchronized (GroovyLogManager.manager) {
				GroovyLogManager.manager.removeLogger(this);
			}
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

		@Override
		public boolean isCategoryEnabled(TraceCategory cat) {
			/*
			 * Note that the compiler errors are just additionally caught here.
			 * They are also printed directly to System.err.
			 * See org.codehaus.jdt.groovy.internal.compiler.ast.GroovyCompilationUnitDeclaration.recordProblems
			 * for details.
			 */
			return TraceCategory.COMPILER.equals(cat);
		}

		@Override
		public void log(TraceCategory arg0, String arg1) {
			errors.add(arg1);
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
