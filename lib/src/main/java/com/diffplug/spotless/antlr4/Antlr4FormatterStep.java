/*
 * Copyright 2016-2024 DiffPlug
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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.RoundedStep;
import com.diffplug.spotless.ThrowingEx;

public class Antlr4FormatterStep implements RoundedStep {
	private static final long serialVersionUID = 1L;
	private static final String MAVEN_COORDINATE = "com.khubla.antlr4formatter:antlr4-formatter:";
	private static final String DEFAULT_VERSION = "1.2.1";
	public static final String NAME = "antlr4Formatter";

	private final JarState.Promised jarState;

	private Antlr4FormatterStep(JarState.Promised jarState) {
		this.jarState = jarState;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(defaultVersion(), provisioner);
	}

	public static FormatterStep create(String version, Provisioner provisioner) {
		return FormatterStep.create(NAME,
				new Antlr4FormatterStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + version, provisioner))),
				Antlr4FormatterStep::equalityState,
				State::createFormat);
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	private State equalityState() {
		return new State(jarState.get());
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;
		private final JarState jarState;

		State(JarState jarState) {
			this.jarState = jarState;
		}

		FormatterFunc createFormat() throws ClassNotFoundException, NoSuchMethodException {
			ClassLoader classLoader = jarState.getClassLoader();

			// String Antlr4Formatter::format(String input)
			Class<?> formatter = classLoader.loadClass("com.khubla.antlr4formatter.Antlr4Formatter");
			Method formatterMethod = formatter.getMethod("format", String.class);

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
