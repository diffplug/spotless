/*
 * Copyright 2016-2023 DiffPlug
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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;

class MultiProjectTest extends GradleIntegrationHarness {
	private static int N = 100;

	private void createNSubprojects() throws IOException {
		for (int i = 0; i < N; ++i) {
			createSubproject(Integer.toString(i));
		}
		String settings = StringPrinter.buildString(printer -> {
			for (int i = 0; i < N; ++i) {
				printer.println("include '" + i + "'");
			}
		});
		setFile("settings.gradle").toContent(settings);
	}

	void createSubproject(String name) throws IOException {
		setFile(name + "/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat('1.17.0')",
				"    }",
				"}");
		setFile(name + "/test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
	}

	@Test
	public void noRootSpotless() throws IOException {
		createNSubprojects();
		setFile("build.gradle").toLines();
		applyIsUpToDate(false);
	}

	@Test
	public void hasRootSpotless() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat('1.17.0')",
				"    }",
				"}");
		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		createNSubprojects();
		applyIsUpToDate(false);
	}

	@Test
	public void predeclaredFails() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless { predeclareDeps() }");
		createNSubprojects();
		Assertions.assertThat(gradleRunner().withArguments("spotlessApply").buildAndFail().getOutput())
				.contains("Add a step with [com.google.googlejavaformat:google-java-format:1.17.0] into the `spotlessPredeclare` block in the root project.");
	}

	@Test
	public void predeclaredSucceeds() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless { predeclareDeps() }",
				"spotlessPredeclare {",
				" java { googleJavaFormat('1.17.0') }",
				"}");
		createNSubprojects();
		gradleRunner().withArguments("spotlessApply").build();
	}

	@Test
	public void predeclaredFromBuildscriptSucceeds() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless { predeclareDepsFromBuildscript() }",
				"spotlessPredeclare {",
				" java { googleJavaFormat('1.17.0') }",
				"}");
		createNSubprojects();
		gradleRunner().withArguments("spotlessApply").build();
	}

	@Test
	public void predeclaredOrdering() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotlessPredeclare {",
				" java { googleJavaFormat('1.17.0') }",
				"}",
				"spotless { predeclareDepsFromBuildscript() }");
		createNSubprojects();
		Assertions.assertThat(gradleRunner().withArguments("spotlessApply").buildAndFail().getOutput())
				.contains("Could not find method spotlessPredeclare() for arguments");
	}

	@Test
	public void predeclaredUndeclared() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotlessPredeclare {",
				" java { googleJavaFormat('1.17.0') }",
				"}");
		createNSubprojects();
		Assertions.assertThat(gradleRunner().withArguments("spotlessApply").buildAndFail().getOutput())
				.contains("Could not find method spotlessPredeclare() for arguments");
	}
}
