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

import org.junit.jupiter.api.Test;

public class TableTestExtensionTest extends GradleIntegrationHarness {
	@Test
	public void defaultFormatting() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'java'",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"spotless {",
				"  tableTest {",
				"    target 'src/**/*.table'",
				"    tableTestFormatter()",
				"  }",
				"}");
		setFile("src/main/resources/example.table").toResource("tableTest/tableFileUnformatted.test");
		setFile("other/example.table").toResource("tableTest/tableFileUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("src/main/resources/example.table").sameAsResource("tableTest/tableFileFormatted.test");
		assertFile("other/example.table").sameAsResource("tableTest/tableFileUnformatted.test");
	}
}
