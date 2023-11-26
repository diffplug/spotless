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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * We test KotlinGradleExtension only behaviors here.
 */
class KotlinGradleExtensionTest extends KotlinExtensionTest {

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testTarget(boolean useDefaultTarget) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'org.jetbrains.kotlin.jvm' version '1.5.31'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    kotlinGradle {",
				"        " + (useDefaultTarget ? "" : "target \"*.kts\""),
				"        ktlint().editorConfigOverride([",
				"            ktlint_experimental: \"enabled\",",
				"            ij_kotlin_allow_trailing_comma: true,",
				"            ij_kotlin_allow_trailing_comma_on_call_site: true",
				"        ])",
				"    }",
				"}");
		setFile("configuration.gradle.kts").toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		setFile("configuration.kts").toResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		gradleRunner().withArguments("spotlessApply").build();
		if (useDefaultTarget) {
			assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
			assertFile("configuration.kts").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.dirty");
		} else {
			assertFile("configuration.gradle.kts").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
			assertFile("configuration.kts").sameAsResource("kotlin/ktlint/experimentalEditorConfigOverride.clean");
		}
	}
}
