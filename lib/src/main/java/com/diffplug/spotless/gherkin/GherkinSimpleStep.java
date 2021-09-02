/*
 * Copyright 2021 DiffPlug
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
package com.diffplug.spotless.gherkin;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class GherkinSimpleStep {
	private static final String MAVEN_COORDINATE = "me.jvt.cucumber:gherkin-formatter:";
	private static final String DEFAULT_VERSION = "1.1.0";

	public static FormatterStep create(int indent, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.createLazy("gherkin", () -> new GherkinSimpleStep.State(indent, provisioner), GherkinSimpleStep.State::toFormatter);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final int indentSpaces;
		private final JarState jarState;

		private State(int indent, Provisioner provisioner) throws IOException {
			this.indentSpaces = indent;
			this.jarState = JarState.from(MAVEN_COORDINATE + DEFAULT_VERSION, provisioner);
		}

		FormatterFunc toFormatter() {
			Method format;
			Object formatter;
			try {
				ClassLoader classLoader = jarState.getClassLoader();
				Class<?> prettyFormatter = classLoader.loadClass("me.jvt.cucumber.gherkinformatter.PrettyFormatter");
				Class<?>[] constructorArguments = new Class[]{int.class};
				Constructor<?> constructor = prettyFormatter.getConstructor(constructorArguments);
				format = prettyFormatter.getMethod("format", String.class);
				formatter = constructor.newInstance(indentSpaces);
			} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new IllegalStateException(String.format("There was a problem preparing %s dependencies", MAVEN_COORDINATE), e);
			}

			return s -> {
				try {
					return (String) format.invoke(formatter, s);
				} catch (InvocationTargetException ex) {
					throw new AssertionError("Unable to format Gherkin", ex.getCause());
				}
			};
		}
	}

	private GherkinSimpleStep() {
		// cannot be directly instantiated
	}
}
