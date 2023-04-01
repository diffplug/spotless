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

import static org.junit.jupiter.api.condition.JRE.JAVA_14;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;

class FreshMarkExtensionTest extends GradleIntegrationHarness {
	@Test
	@EnabledForJreRange(max = JAVA_14)
	void integration() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"    freshmark {",
				"        target '*.md'",
				"        properties {",
				"            it.put('lib', 'MyLib')",
				"            it.put('author', 'Me')",
				"        }",
				"    }",
				"}");
		setFile("README.md").toResource("freshmark/FreshMarkUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("README.md").sameAsResource("freshmark/FreshMarkFormatted.test");
	}
}
