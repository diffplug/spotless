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
package com.diffplug.spotless.yaml;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.JarState;
import com.diffplug.spotless.Provisioner;

/**
 * Simple YAML formatter which reformats the file according to Jackson YAMLFactory.
 */
// https://stackoverflow.com/questions/14515994/convert-json-string-to-pretty-print-json-output-using-jackson
public class YamlJacksonStep {
	static final String MAVEN_COORDINATE = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:";
	// https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml
	static final String DEFAULT_VERSION = "2.14.1";

	private YamlJacksonStep() {}

	public static String defaultVersion() {
		return DEFAULT_VERSION;
	}

	public static FormatterStep create(List<String> enabledFeatures,
			List<String> disabledFeatures,
			String jacksonVersion,
			Provisioner provisioner) {
		Objects.requireNonNull(provisioner, "provisioner cannot be null");
		return FormatterStep.createLazy("yaml",
				() -> new State(enabledFeatures, disabledFeatures, jacksonVersion, provisioner),
				State::toFormatter);
	}

	public static FormatterStep create(Provisioner provisioner) {
		return create(Arrays.asList("INDENT_OUTPUT"), Arrays.asList(), defaultVersion(), provisioner);
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 1L;

		private final List<String> enabledFeatures;
		private final List<String> disabledFeatures;

		private final JarState jarState;

		private State(List<String> enabledFeatures,
				List<String> disabledFeatures,
				String jacksonVersion,
				Provisioner provisioner) throws IOException {
			this.enabledFeatures = enabledFeatures;
			this.disabledFeatures = disabledFeatures;

			this.jarState = JarState.from(YamlJacksonStep.MAVEN_COORDINATE + jacksonVersion, provisioner);
		}

		FormatterFunc toFormatter() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
				InstantiationException, IllegalAccessException {
			Class<?> formatterFunc = jarState.getClassLoader().loadClass("com.diffplug.spotless.glue.yaml.YamlJacksonFormatterFunc");
			Constructor<?> constructor = formatterFunc.getConstructor(List.class, List.class);
			return (FormatterFunc) constructor.newInstance(enabledFeatures, disabledFeatures);
		}
	}
}
