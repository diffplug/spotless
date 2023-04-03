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
package com.diffplug.spotless.antlr4;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

import com.diffplug.spotless.*;

public class Antlr4FormatterStep {

	public static final String NAME = "antlr4Formatter";

	private Antlr4FormatterStep() {}

	private static final String MAVEN_COORDINATE = "com.khubla.antlr4formatter:antlr4-formatter:";
	private static final String DEFAULT_VERSION = "1.2.1";

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return FormatterStep.createLazy(NAME, () -> new State(version, provisioner), State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		/**
		 * The jar that contains the formatter.
		 */
		final JarState jarState;

		State(String version, Provisioner provisioner) throws IOException {
			this.jarState = JarState.from(MAVEN_COORDINATE + version, provisioner);
		}

		FormatterFunc createFormat() throws ClassNotFoundException, NoSuchMethodException {
			ClassLoader classLoader = jarState.getClassLoader();

			// String Antlr4Formatter::format(String input)
			Class<?> formatter = classLoader.loadClass("com.khubla.antlr4formatter.Antlr4Formatter");
			var formatterMethod = formatter.getMethod("format", String.class);

			return input -> {
				try {
					return (String) formatterMethod.invoke(null, input);
				} catch (InvocationTargetException e) {
					throw ThrowingEx.unwrapCause(e);
				}
			};
		}
	}
}
