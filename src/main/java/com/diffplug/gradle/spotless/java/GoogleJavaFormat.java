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

import org.gradle.api.Project;

import com.diffplug.common.base.Throwing;
import com.diffplug.gradle.spotless.FormatterStep;
import com.diffplug.gradle.spotless.JarState;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
class GoogleJavaFormat {
	static final String DEFAULT_VERSION = "1.1";
	private static final String NAME = "google-java-format";
	private static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
	private static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
	private static final String FORMATTER_METHOD = "formatSource";

	/** Creates a formatter step for the given version and settings file. */
	public static FormatterStep createStep(String version, Project project) {
		return FormatterStep.createLazy(NAME,
				() -> new State(new JarState(MAVEN_COORDINATE + version, project)),
				State::createFormat);
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(JarState jarState) {
			this.jarState = Objects.requireNonNull(jarState);
		}

		Throwing.Function<String, String> createFormat() throws Exception {
			URLClassLoader classLoader = jarState.openIsolatedClassLoader();
			// TODO: dispose the classloader when the function
			// that we return gets garbage-collected

			// instantiate the formatter and get its format method
			Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
			Object formatter = formatterClazz.getConstructor().newInstance();
			Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
			return input -> (String) method.invoke(formatter, input);
		}
	}
}
