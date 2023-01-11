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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
public final class YamlJacksonStep {
	private static final String MAVEN_COORDINATE = "com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:";
	private static final String DEFAULT_VERSION = "2.13.4";

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

			this.jarState = JarState.from(MAVEN_COORDINATE + jacksonVersion, provisioner);
		}

		FormatterFunc toFormatter() {
			Class<?> jsonFactoryClass;
			Class<?> yamlFactoryClass;
			Class<?> objectMapperClass;

			Class<?> serializationFeatureClass;
			Method enableFeature;
			Method disableFeature;

			Method stringToObject;
			Method objectToString;
			try {
				ClassLoader classLoader = jarState.getClassLoader();
				jsonFactoryClass = classLoader.loadClass("com.fasterxml.jackson.core.JsonFactory");
				yamlFactoryClass = classLoader.loadClass("com.fasterxml.jackson.dataformat.yaml.YAMLFactory");

				objectMapperClass = classLoader.loadClass("com.fasterxml.jackson.databind.ObjectMapper");

				// Configure the ObjectMapper
				// https://github.com/FasterXML/jackson-databind#commonly-used-features
				{
					serializationFeatureClass = classLoader.loadClass("com.fasterxml.jackson.databind.SerializationFeature");
					enableFeature = objectMapperClass.getMethod("enable", serializationFeatureClass);
					disableFeature = objectMapperClass.getMethod("disable", serializationFeatureClass);
				}

				stringToObject = objectMapperClass.getMethod("readValue", String.class, Class.class);
				objectToString = objectMapperClass.getMethod("writeValueAsString", Object.class);
			} catch (ClassNotFoundException | NoSuchMethodException e) {
				throw new IllegalStateException("There was a problem preparing org.json dependencies", e);
			}

			return s -> {
				if (s.isEmpty()) {
					return s;
				}

				Object yamlFactory = yamlFactoryClass.getConstructor().newInstance();
				Object objectMapper = objectMapperClass.getConstructor(jsonFactoryClass).newInstance(yamlFactory);

				for (String feature : enabledFeatures) {
					// https://stackoverflow.com/questions/3735927/java-instantiating-an-enum-using-reflection
					Object indentOutput = Enum.valueOf(serializationFeatureClass.asSubclass(Enum.class), feature);

					enableFeature.invoke(objectMapper, indentOutput);
				}

				for (String feature : disabledFeatures) {
					// https://stackoverflow.com/questions/3735927/java-instantiating-an-enum-using-reflection
					Object indentOutput = Enum.valueOf(serializationFeatureClass.asSubclass(Enum.class), feature);

					disableFeature.invoke(objectMapper, indentOutput);
				}

				return format(objectMapper, stringToObject, objectToString, s);
			};
		}

		private String format(Object objectMapper, Method stringToObject, Method objectToString, String s)
				throws IllegalAccessException, IllegalArgumentException {
			try {
				Object parsed = stringToObject.invoke(objectMapper, s, Object.class);
				return (String) objectToString.invoke(objectMapper, parsed);
			} catch (InvocationTargetException ex) {
				throw new AssertionError("Unable to format YAML", ex.getCause());
			}
		}
	}

	private YamlJacksonStep() {
		// cannot be directly instantiated
	}
}
