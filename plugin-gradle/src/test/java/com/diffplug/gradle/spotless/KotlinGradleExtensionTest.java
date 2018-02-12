/*
 * Copyright 2016 DiffPlug
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

import org.junit.Assert;
import org.junit.Test;

public class KotlinGradleExtensionTest extends GradleIntegrationTest {
	@Test
	public void integration() throws IOException {
		testInDirectory(null);
	}

	@Test
	public void integration_script_in_subdir() throws IOException {
		testInDirectory("companionScripts");
	}

	private void testInDirectory(final String directory) throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'nebula.kotlin' version '1.0.6'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint()",
				"        target '**/*.gradle.kts'",
				"    }",
				"}");
		String filePath = "configuration.gradle.kts";
		if (directory != null) {
			filePath = directory + "/" + filePath;
		}
		write(filePath,
				getTestResource("kotlin/ktlint/basic.dirty"));
		gradleRunner().withArguments("spotlessApply").build();
		String result = read(filePath);
		String formatted = getTestResource("kotlin/ktlint/basic.clean");
		Assert.assertEquals(formatted, result);
	}

	@Test
	public void integration_default() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'nebula.kotlin' version '1.0.6'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint()",
				"    }",
				"}");
		write("configuration.gradle.kts",
				getTestResource("kotlin/ktlint/basic.dirty"));
		gradleRunner().withArguments("spotlessApply").build();
		String result = read("configuration.gradle.kts");
		String formatted = getTestResource("kotlin/ktlint/basic.clean");
		Assert.assertEquals(formatted, result);
	}

	@Test
	public void integration_lint_script_files_without_top_level_declaration() throws IOException {
		write("build.gradle",
				"plugins {",
				"    id 'nebula.kotlin' version '1.0.6'",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint()",
				"    }",
				"}");
		write("configuration.gradle.kts", "buildscript {}");
		gradleRunner().withArguments("spotlessApply").build();
		String result = read("configuration.gradle.kts");
		String formatted = "buildscript {}";
		Assert.assertEquals(formatted, result);
	}
}
