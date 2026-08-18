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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RegisterDependenciesTaskBuildDirTest extends GradleIntegrationHarness {
	@Test
	void unitOutputFollowsCustomBuildDirectory() throws IOException {
		setFile("settings.gradle").toLines(
				"rootProject.name = 'buildDirTest'",
				"include 'sub'");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless { predeclareDeps() }",
				"",
				"spotlessPredeclare {",
				"    java { googleJavaFormat('1.17.0') }",
				"}",
				"",
				"layout.buildDirectory = layout.projectDirectory.dir('custom-build')");
		setFile("sub/build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    java {",
				"        target 'src/main/java/**/*.java'",
				"        googleJavaFormat('1.17.0')",
				"    }",
				"}");
		setFile("sub/src/main/java/Hello.java").toLines(
				"public class Hello {}");

		gradleRunner().withArguments("spotlessApply").build();

		Assertions.assertThat(newFile("custom-build/tmp/spotless-register-dependencies"))
				.as("register-dependencies output should follow the configured build directory")
				.exists();
		Assertions.assertThat(newFile("build/tmp/spotless-register-dependencies"))
				.as("nothing should be written under the default build directory")
				.doesNotExist();
	}
}
