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
package com.diffplug.spotless.kotlin;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.function.BiFunction;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/** Wraps up [google-java-format](https://github.com/google/google-java-format) as a FormatterStep. */
public class KtLintFormatStep {
	// prevent direct instantiation
	private KtLintFormatStep() {}

	private static final String DEFAULT_VERSION = "0.2.2";
	static final String NAME = "ktlint";
	static final String MAVEN_COORDINATE = "com.github.shyiko:ktlint:";

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	/** Creates a step which formats everything - code, import order, and unused imports. */
	public static FormatterStep create(String version, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME,
				() -> new State(version, provisioner),
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/** The jar that contains the eclipse formatter. */
		final JarState jarState;

		State(String version, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
		}

		FormatterFunc createFormat() throws Exception {
			ClassLoader classLoader = jarState.getClassLoader();

			Class<?> function2clazz = classLoader.loadClass("kotlin.jvm.functions.Function2");

			// instantiate the formatter and get its format method
			Class<?> formatterClazz = classLoader.loadClass("com.github.shyiko.ktlint.core.KtLint");
			Method formatterMethod = formatterClazz.getMethod("format", String.class, Iterable.class, function2clazz);

			// Correct logic to get the rules
			// https://github.com/shyiko/ktlint/blob/948f298cdd7186b8b4af80591518abb637592302/ktlint/src/main/kotlin/com/github/shyiko/ktlint/Main.kt#L182-L184
			Iterable<Object> rules = Collections.emptyList();
			BiFunction<Object, Object, Object> dummyCallback = (a, b) -> null;
			return input -> {
				String formatted = (String) formatterMethod.invoke(null, input, rules, dummyCallback);
				return formatted;
			};
		}
	}
}
