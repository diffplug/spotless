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
package com.diffplug.spotless.json;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/**
 * Simple YAML formatter which reformats the file according to Jackson YAMLFactory.
 */
// https://stackoverflow.com/questions/14515994/convert-json-string-to-pretty-print-json-output-using-jackson
public class JacksonJsonStep implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final String MAVEN_COORDINATE = "com.fasterxml.jackson.core:jackson-databind:";
	private static final String DEFAULT_VERSION = "2.17.2";
	public static final String NAME = "jacksonJson";

	private final JarState.Promised jarState;
	private final JacksonConfig jacksonConfig;

	private JacksonJsonStep(JarState.Promised jarState, JacksonConfig jacksonConfig) {
		this.jarState = jarState;
		this.jacksonConfig = jacksonConfig;
	}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(new JacksonJsonConfig(), defaultVersion(), provisioner);
	}

	public static FormatterStep create(JacksonJsonConfig jacksonConfig,
			String jacksonVersion,
			Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.create(NAME,
				new JacksonJsonStep(JarState.promise(() -> JarState.from(MAVEN_COORDINATE + jacksonVersion, provisioner)), jacksonConfig),
				JacksonJsonStep::equalityState,
				State::toFormatter);
	}

	private State equalityState() {
		return new State(jarState.get(), jacksonConfig);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final JacksonConfig jacksonConfig;
		private final JarState jarState;

		State(JarState jarState, JacksonConfig jacksonConfig) {
			this.jarState = jarState;
			this.jacksonConfig = jacksonConfig;
		}

		FormatterFunc toFormatter() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
				InstantiationException, IllegalAccessException {
			Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.json.JacksonJsonFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(JacksonJsonConfig.class);
			return (FormatterFunc) constructor.newInstance(jacksonConfig);
		}
	}
}
