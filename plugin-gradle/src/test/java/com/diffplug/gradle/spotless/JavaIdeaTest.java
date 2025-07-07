/*
 * Copyright 2020-2025 DiffPlug
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

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.tag.IdeaTest;

@IdeaTest
class JavaIdeaTest extends GradleIntegrationHarness {
	@Test
	void idea() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  java {",
				"    target file('test.java')",
				"    idea().binaryPath('idea').withDefaults(true)",
				"  }",
				"}");

		setFile("test.java").toResource("java/idea/full.dirty.java");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").notSameAsResource("java/idea/full.dirty.java");
	}
}
