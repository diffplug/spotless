/*
 * Copyright 2021-2023 DiffPlug
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
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

public class GherkinSimpleStep {
	private static final String MAVEN_COORDINATE = "io.cucumber:gherkin-utils:";
	private static final String DEFAULT_VERSION = "8.0.2";

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	public static FormatterStep create(GherkinSimpleConfig gherkinSimpleConfig,
			String formatterVersion, Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.createLazy("gherkin", () -> new GherkinSimpleStep.State(gherkinSimpleConfig, formatterVersion, provisioner), GherkinSimpleStep.State::toFormatter);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final GherkinSimpleConfig gherkinSimpleConfig;
		private final JarState jarState;

		private State(GherkinSimpleConfig gherkinSimpleConfig, String formatterVersion, Provisioner provisioner) throws IOException {
			this.gherkinSimpleConfig = gherkinSimpleConfig;
			this.jarState = JarState.from(MAVEN_COORDINATE + formatterVersion, provisioner);
		}

		FormatterFunc toFormatter() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
				InstantiationException, IllegalAccessException {
			Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.gherkin.GherkinFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(GherkinSimpleConfig.class);
			return (FormatterFunc) constructor.newInstance(gherkinSimpleConfig);
		}
	}

	private GherkinSimpleStep() {
		// cannot be directly instantiated
	}
}
