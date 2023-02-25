/*
 * Copyright 2020-2021 DiffPlug
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

class ToggleOffOnTest extends GradleIntegrationHarness {
	@Test
	void toggleOffOn() throws IOException {
		setFile("build.gradle").toLines(
				"plugins { id 'com.diffplug.spotless' }",
				"spotless {",
				"  format 'toLower', {",
				"    target '**/*.md'",
				"    custom 'lowercase', { str -> str.toLowerCase() }",
				"    toggleOffOn()",
				"  }",
				"}");
		setFile("test.md").toLines(
				"A B C",
				"spotless:off",
				"D E F",
				"spotless:on",
				"G H I");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.md").hasLines(
				"a b c",
				"spotless:off",
				"D E F",
				"spotless:on",
				"g h i");
	}
}
