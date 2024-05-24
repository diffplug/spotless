/*
 * Copyright 2021-2024 DiffPlug
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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class GherkinUtilsStep implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static final String MAVEN_COORDINATE = "io.cucumber:gherkin-utils:";
	private static final String DEFAULT_VERSION = "9.0.0";
	public static final String NAME = "gherkinUtils";

	private final JarState.Promised jarState;
	private final GherkinUtilsConfig gherkinSimpleConfig;

	private GherkinUtilsStep(JarState.Promised jarState, GherkinUtilsConfig gherkinSimpleConfig) {
		this.jarState = jarState;
		this.gherkinSimpleConfig = gherkinSimpleConfig;
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	public static FormatterStep create(GherkinUtilsConfig gherkinSimpleConfig,
			String formatterVersion, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.create(NAME,
				new GherkinUtilsStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + formatterVersion, provisioner)), gherkinSimpleConfig),
				GherkinUtilsStep::equalityState,
				GherkinUtilsStep.State::toFormatter);
	}

	private State equalityState() {
		return new State(jarState.get(), gherkinSimpleConfig);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final GherkinUtilsConfig gherkinSimpleConfig;
		private final JarState jarState;

		State(JarState jarState, GherkinUtilsConfig gherkinSimpleConfig) {
			this.jarState = jarState;
			this.gherkinSimpleConfig = gherkinSimpleConfig;
		}

		FormatterFunc toFormatter() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
				InstantiationException, IllegalAccessException {
			Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.gherkin.GherkinUtilsFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(GherkinUtilsConfig.class);
			return (FormatterFunc) constructor.newInstance(gherkinSimpleConfig);
		}
	}
}
