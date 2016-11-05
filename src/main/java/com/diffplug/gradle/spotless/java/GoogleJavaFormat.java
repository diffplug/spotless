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

import java.lang.reflect.Method;
import java.net.URLClassLoader;

import com.diffplug.common.base.Throwing;
import com.diffplug.gradle.spotless.JarState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
class GoogleJavaFormat {
	static final String NAME = "google-java-format";
	static final String DEFAULT_VERSION = "1.1";
	static final String MAVEN_COORDINATE = "com.google.googlejavaformat:google-java-format:";
	static final String FORMATTER_CLASS = "com.google.googlejavaformat.java.Formatter";
	static final String FORMATTER_METHOD = "formatSource";

	/** Returns a function which will call the google-java-format tool. */
	static Throwing.Specific.Function<String, String, Exception> createRule(JarState state) throws Exception {
		URLClassLoader classLoader = state.openClassLoader();
		// TODO: dispose the classloader when the function
		// that we return gets garbage-collected

		// instantiate the formatter and get its format method
		Class<?> formatterClazz = classLoader.loadClass(FORMATTER_CLASS);
		Object formatter = formatterClazz.getConstructor().newInstance();
		Method method = formatterClazz.getMethod(FORMATTER_METHOD, String.class);
		return input -> (String) method.invoke(formatter, input);
	}
}
