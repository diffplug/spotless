/*
 * Copyright 2026 DiffPlug
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

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;

class IsolatedProjectTest extends GradleIntegrationHarness {
	private static final int N = 10;

	@Override
	public GradleRunner gradleRunner() throws IOException {
		setFile("gradle.properties").toContent("org.gradle.unsafe.isolated-projects=true");
		return super.gradleRunner();
	}

	private void createNSubprojects() throws IOException {
		for (int i = 0; i < N; i++) {
			createSubproject(Integer.toString(i));
		}
		String settings = StringPrinter.buildString(printer -> {
			for (int i = 0; i < N; i++) {
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
				"spotless {",
				"    kotlin {",
				"		 target file('Test.kt')",
				"        ktlint()",
				"    }",
				"}");
		setFile(name + "/Test.kt").toResource("kotlin/ktlint/basic.dirty");
	}

	@Test
	void rootIsSupported() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlin {",
				"		 target file('Test.kt')",
				"        ktlint()",
				"    }",
				"}");
		setFile("Test.kt").toResource("kotlin/ktlint/basic.dirty");
		createNSubprojects();
		gradleRunner().withArguments("spotlessApply").build();
	}

	@Test
	void noRootIsSupported() throws IOException {
		setFile("build.gradle").toLines();
		createNSubprojects();
		gradleRunner().withArguments("spotlessApply").build();
	}

	@Test
	void predeclaredIsSupported() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless { predeclareDeps() }",
				"spotlessPredeclare {",
				" kotlin { ktlint() }",
				"}");
		createNSubprojects();
		gradleRunner().withArguments("spotlessApply").build();
	}
}
