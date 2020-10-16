/*
 * Copyright 2020 DiffPlug
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
package com.diffplug.gradle.spotless;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ConfigurationCacheTest extends GradleIntegrationHarness {
	protected void runTasks(String... tasks) throws IOException {
		setFile("gradle.properties").toContent("org.gradle.unsafe.configuration-cache=true");
		List<String> args = new ArrayList<>();
		args.addAll(Arrays.asList(tasks));
		gradleRunner()
				.withGradleVersion(GradleVersionSupport.CONFIGURATION_CACHE.version)
				.withArguments(args)
				.forwardOutput()
				.build();
	}

	@Test
	public void helpConfigures() throws IOException {
		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"apply plugin: 'java'",
				"spotless {",
				"    java {",
				"        googleJavaFormat('1.2')",
				"    }",
				"}");
		runTasks("help");
	}
}
