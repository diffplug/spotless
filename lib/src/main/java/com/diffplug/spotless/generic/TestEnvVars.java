/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;

class TestEnvVars {

	private final Map<String, String> envVars;

	private static TestEnvVars INSTANCE;

	private TestEnvVars(Map<String, String> envVars) {
		this.envVars = Map.copyOf(envVars);
	}

	public static synchronized TestEnvVars read() {
		if (INSTANCE == null) {
			INSTANCE = new TestEnvVars(readTestEnvVars());
		}
		return INSTANCE;
	}

	private static Map<String, String> readTestEnvVars() {
		Map<String, String> envVars = new HashMap<>();
		Optional<Path> resolvedTestenvProps = candidateTestEnvLocations().filter(Files::exists).findFirst();
		resolvedTestenvProps.ifPresent(testenvProps -> {
			try (var reader = Files.newBufferedReader(testenvProps)) {
				java.util.Properties properties = new java.util.Properties();
				properties.load(reader);
				for (String name : properties.stringPropertyNames()) {
					envVars.put(name, properties.getProperty(name));
				}
			} catch (IOException e) {
				throw new RuntimeException("Failed to read test environment variables", e);
			}
		});
		return envVars;
	}

	private static Stream<Path> candidateTestEnvLocations() {
		Stream.Builder<Path> builder = Stream.builder();
		if (System.getProperty("testenv.properties.path") != null) {
			builder.add(Path.of(System.getProperty("testenv.properties.path")));
		}
		builder.add(
				Path.of(System.getProperty("user.dir"), "testenv.properties"));
		builder.add(
				Path.of(System.getProperty("user.dir")).getParent().resolve("testenv.properties"));
		return builder.build();
	}

	public @Nullable String get(String key) {
		return envVars.get(key);
	}

	public String getOrDefault(String key, String defaultValue) {
		return envVars.getOrDefault(key, defaultValue);
	}

	public String getOrThrow(String key) {
		String value = envVars.get(key);
		if (value == null) {
			throw new IllegalArgumentException("Environment variable " + key + " not found");
		}
		return value;
	}

	public boolean hasKey(String key) {
		return envVars.containsKey(key);
	}
}
