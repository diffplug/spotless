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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Objects;

import com.diffplug.gradle.spotless.FormatterFunc;
import com.diffplug.gradle.spotless.FormatterStep;
import com.diffplug.gradle.spotless.JarState;
import com.diffplug.gradle.spotless.Provisioner;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
class GoogleJavaFormat {
	static final String DEFAULT_VERSION = "1.1";
	private static final String NAME = "google-java-format";
	private static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
	private static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
	private static final String FORMATTER_METHOD = "formatSource";

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep createStep(String version, Provisioner provisioner) {
		return FormatterStep.createCloseableLazy(NAME,
				() -> new State(new JarState(MAVEN_COORDINATE + version, provisioner)),
				State::createFormat);
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(JarState jarState) {
			this.jarState = Objects.requireNonNull(jarState);
		}

		FormatterFunc.Closeable createFormat() throws Exception {
			URLClassLoader classLoader = jarState.openIsolatedClassLoader();

			// instantiate the formatter and get its format method
			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor().newInstance();
			Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
			return FormatterFunc.Closeable.of(classLoader, input -> (String) method.invoke(formatter, input));
		}
	}
}
