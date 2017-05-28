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
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterProperties;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Formatter step which calls out to the Eclipse formatter. */
public final class EclipseFormatterStep {
	// prevent direct instantiation
	private EclipseFormatterStep() {}

	private static final String DEFAULT_VERSION = "4.6.3";
	private static final String NAME = "eclipse formatter";
	private static final String MAVEN_COORDINATE = "com.diffplug.spotless:spotless-ext-eclipse-jdt:";
	private static final String FORMATTER_CLASS = "com.diffplug.gradle.spotless.java.eclipse.EclipseFormatterStepImpl";
	private static final String FORMATTER_METHOD = "format";

	/** Creates a formatter step for the given version and settings file.
	 * Formatter steps based on property configuration should support zero (default configuration)
	 * to many files. Use {@link #create(Iterable, Provisioner)} instead.*/
	@Deprecated
	public static FormatterStep create(File settingsFile, Provisioner provisioner) {
		return create(Collections.singletonList(settingsFile), provisioner);
	}

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep create(Iterable<File> settingsFiles, Provisioner provisioner) {
		return create(defaultVersion(), settingsFiles, provisioner);
	}

	/** Creates a formatter step for the given version and settings file.
	 * Formatter steps based on property configuration should support zero (default configuration)
	 * to many files. Use {@link #create(String, Iterable, Provisioner)} instead.*/
	@Deprecated
	public static FormatterStep create(String version, File settingsFile, Provisioner provisioner) {
		return create(version, Collections.singletonList(settingsFile), provisioner);
	}

	/** Creates a formatter step for the given version and settings files. */
	public static FormatterStep create(String version, Iterable<File> settingsFiles, Provisioner provisioner) {
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(settingsFiles, "settingsFiles");
		Objects.requireNonNull(provisioner, "provisioner");
		return FormatterStep.createLazy(NAME,
				() -> new State(JarState.from(MAVEN_COORDINATE + version, provisioner), settingsFiles),
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

		State(JarState jar, final Iterable<File> settingsFiles) throws Exception {
			this.jarState = jar;
			this.settings = FileSignature.signAsList(settingsFiles);
		}

		FormatterFunc createFormat() throws Exception {
			FormatterProperties preferences = FormatterProperties.from(settings.files());

			ClassLoader classLoader = jarState.getClassLoader();

			// instantiate the formatter and get its format method
			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor(Properties.class).newInstance(preferences.getProperties());
			Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
			return input -> (String) method.invoke(formatter, input);
		}
	}

}
