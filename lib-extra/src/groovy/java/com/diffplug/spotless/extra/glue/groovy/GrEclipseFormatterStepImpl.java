/*
 * Copyright 2016-2026 DiffPlug
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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.eclipse.osgi.internal.location.EquinoxLocations;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.equo.solstice.NestedJars;
import dev.equo.solstice.ShimIdeBootstrapServices;
import dev.equo.solstice.Solstice;
import dev.equo.solstice.p2.CacheLocations;

/** Spotless-Formatter step which calls out to the Groovy-Eclipse formatter. */
public class GrEclipseFormatterStepImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(GrEclipseFormatterStepImpl.class);

	private static Logger warnAsInfo(Logger delegate) {
		return (Logger) Proxy.newProxyInstance(
				Logger.class.getClassLoader(),
				new Class<?>[]{Logger.class},
				(proxy, method, args) -> {
					String name = method.getName();

					if ("warn".equals(name)) {
						return Logger.class
								.getMethod("info", method.getParameterTypes())
								.invoke(delegate, args);
					}
					if ("isWarnEnabled".equals(name)) {
						return Logger.class
								.getMethod("isInfoEnabled", method.getParameterTypes())
								.invoke(delegate, args);
					}

					return method.invoke(delegate, args);
				});
	}

	static {
		NestedJars.setToWarnOnly();
		NestedJars.onClassPath().confirmAllNestedJarsArePresentOnClasspath(nestedJarFolder());

		try {
			var solstice = Solstice.findBundlesOnClasspath();

			// Missing OSGi requirements are expected in the minimal Eclipse runtime
			// used by the formatter. Solstice removes them before starting it.
			solstice.warnAndModifyManifestsToFix(warnAsInfo(LOGGER));

			var props = Map.of(
					"osgi.nl", "en_US",
					Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT,
					EquinoxLocations.PROP_INSTANCE_AREA,
					Files.createTempDirectory("spotless-groovy-instance").toAbsolutePath().toString(),
					EquinoxLocations.PROP_CONFIG_AREA,
					Files.createTempDirectory("spotless-groovy-config").toAbsolutePath().toString());

			solstice.openShim(props);
			ShimIdeBootstrapServices.apply(props, solstice.getContext());
			solstice.start("org.apache.felix.scr");
			solstice.startAllWithLazy(false);
			solstice.start("org.codehaus.groovy.eclipse.core");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static File nestedJarFolder() {
		ClassLoader classLoader = GrEclipseFormatterStepImpl.class.getClassLoader();
		if (classLoader instanceof URLClassLoader urlClassLoader) {
			for (var url : urlClassLoader.getURLs()) {
				if (!"file".equals(url.getProtocol())) {
					continue;
				}
				try {
					File file = new File(url.toURI());
					File parent = file.getParentFile();
					if (parent != null && NestedJars.DIR.equals(parent.getName())) {
						return parent;
					}
				} catch (URISyntaxException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return CacheLocations.p2nestedJars();
	}

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
		PreferenceStore preferences = createPreferences(properties);
		preferencesStore = new FormatterPreferencesOnStore(preferences);
		ignoreFormatterProblems = Boolean.parseBoolean(properties.getProperty(IGNORE_FORMATTER_PROBLEMS, "false"));
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
	private static final class GroovyErrorListener implements ILogListener, IGroovyLogger {
		private final List<Throwable> errors;

		public GroovyErrorListener() {
			/*
			 * We need a synchronized list here, in case multiple instantiations
			 * run in parallel.
			 */
			errors = Collections.synchronizedList(new ArrayList<>());
			ILog groovyLogger = GroovyCoreActivator.getDefault().getLog();
			groovyLogger.addLogListener(this);
			synchronized (GroovyLogManager.manager) {
				GroovyLogManager.manager.addLogger(this);
			}
		}

		@Override
		public void logging(final IStatus status, final String plugin) {
			errors.add(status.getException());
		}

		public boolean errorsDetected() {
			ILog groovyLogger = GroovyCoreActivator.getDefault().getLog();
			groovyLogger.removeLogListener(this);
			synchronized (GroovyLogManager.manager) {
				GroovyLogManager.manager.removeLogger(this);
			}
			return !errors.isEmpty();
		}

		@Override
		public String toString() {
			StringBuilder string = new StringBuilder();
			if (1 < errors.size()) {
				string.append("Multiple problems detected during step execution:");
			} else if (errors.isEmpty()) {
				string.append("Step sucesfully executed.");
			}
			for (Throwable error : errors) {
				error.printStackTrace();
				string.append(System.lineSeparator());
				string.append(error.getMessage());
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
			try {
				throw new RuntimeException(arg1);
			} catch (RuntimeException e) {
				errors.add(e);
			}
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
