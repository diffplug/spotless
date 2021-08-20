/*
 * Copyright 2016-2021 DiffPlug
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

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Test;

import com.diffplug.spotless.JreVersion;

public class KotlinGradleExtensionTest extends GradleIntegrationHarness {
	@Test
	public void integration() throws IOException {
		testInDirectory(null);
	}

	@Test
	public void integration_script_in_subdir() throws IOException {
		testInDirectory("companionScripts");
	}

	private void testInDirectory(final String directory) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
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
	public void integration_default() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
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
	public void integration_default_diktat() throws IOException {
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
		BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
		assertThat(result.getOutput()).contains("[HEADER_MISSING_IN_NON_SINGLE_CLASS_FILE] files that contain multiple "
				+ "or no classes should contain description of what is inside of this file: there are 0 declared classes and/or objects");
	}

	@Test
	public void integration_pinterest() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint('0.21.0')",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktlint/basic.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktlint/basic.clean");
	}

	@Test
	public void indentStep() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
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
	public void integration_ktfmt() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
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
	public void integration_ktfmt_with_dropbox_style() throws IOException {
		// ktfmt's dependency, google-java-format 1.8 requires a minimum of JRE 11+.
		JreVersion.assume11OrGreater();
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
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
	public void integration_lint_script_files_without_top_level_declaration() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'nebula.kotlin' version '1.3.72'",
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
