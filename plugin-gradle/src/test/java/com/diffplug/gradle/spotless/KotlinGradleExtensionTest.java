/*
 * Copyright 2016-2022 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.JRE.JAVA_11;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

class KotlinGradleExtensionTest extends GradleIntegrationHarness {
	@Test
	void integration() throws IOException {
		testInDirectory(null);
	}

	@Test
	void integration_script_in_subdir() throws IOException {
		testInDirectory("companionScripts");
	}

	private void testInDirectory(final String directory) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
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
		setFile(filePath).toResource("kotlin/ktlint/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile(filePath).sameAsResource("kotlin/ktlint/basic.clean");
	}

	@Test
	void integration_default() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint()",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktlint/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktlint/basic.clean");
	}

	@Test
	void integration_default_diktat() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.4.30'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        diktat()",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/diktat/basic.dirty");
		BuildResult result = gradleRunner().withArguments("spotlessCheck").buildAndFail();
		assertThat(result.getOutput()).contains("[AVOID_NESTED_FUNCTIONS] try to avoid using nested functions");
	}

	@Test
	void integration_pinterest() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint('0.32.0')",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktlint/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktlint/basic.clean");
	}

	@Test
	void indentStep() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint().userData(['indent_size': '6'])",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktlint/basic.dirty");
		gradleRunner().withArguments("spotlessCheck").buildAndFail();
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void integration_ktfmt() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktfmt()",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktfmt/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktfmt/basic.clean");
	}

	@Test
	@EnabledForJreRange(min = JAVA_11) // ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
	void integration_ktfmt_with_dropbox_style() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktfmt().dropboxStyle()",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktfmt/dropboxstyle.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktfmt/dropboxstyle.clean");
	}

	@Test
	void integration_lint_script_files_without_top_level_declaration() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint()",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toContent("buildscript {}");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("configuration.gradle.kts").hasContent("buildscript {}");
	}
}
