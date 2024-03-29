/*
 * Copyright 2022-2024 DiffPlug
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

class PalantirJavaFormatIntegrationTest extends GradleIntegrationHarness {
	@Test
	void integration() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        palantirJavaFormat('1.1.0')",
				"    }",
				"}");

		setFile("test.java").toResource("java/palantirjavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/palantirjavaformat/JavaCodeFormatted.test");

		checkRunsThenUpToDate();
		replace("build.gradle",
				"palantirJavaFormat('1.1.0')",
				"palantirJavaFormat('1.0.1')");
		checkRunsThenUpToDate();
	}

	@Test
	void formatJavaDoc() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        palantirJavaFormat('2.39.0').formatJavadoc(true)",
				"    }",
				"}");

		setFile("test.java").toResource("java/palantirjavaformat/JavaCodeWithJavaDocUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/palantirjavaformat/JavaCodeWithJavaDocFormatted.test");

		checkRunsThenUpToDate();
	}
}
