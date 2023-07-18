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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

class KotlinGradleExtensionTest extends GradleIntegrationHarness {
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
		BuildResult result = gradleRunner().withArguments("spotlessApply").buildAndFail();
		assertThat(result.getOutput()).contains("[AVOID_NESTED_FUNCTIONS] try to avoid using nested functions");
	}

	@Test
	void withExperimentalEditorConfigOverride() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        ktlint().editorConfigOverride([",
				"            ktlint_experimental: \"enabled\",",
				"            ij_kotlin_allow_trailing_comma: true,",
				"            ij_kotlin_allow_trailing_comma_on_call_site: true",
				"        ])",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
	}

	@Test
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
}
