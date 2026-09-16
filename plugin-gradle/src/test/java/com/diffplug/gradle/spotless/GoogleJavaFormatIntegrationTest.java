/*
 * Copyright 2016-2026 DiffPlug
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

import com.diffplug.spotless.Jvm;

class GoogleJavaFormatIntegrationTest extends GradleIntegrationHarness {
	private static final String NON_DEFAULT_VERSION = Jvm.version() <= 17 ? "1.27.0" : "1.34.1";

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
				"        googleJavaFormat('" + NON_DEFAULT_VERSION + "')",
				"    }",
				"}");

		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");

		checkRunsThenUpToDate();
		replace("build.gradle",
				"googleJavaFormat('" + NON_DEFAULT_VERSION + "')",
				"googleJavaFormat()");
		checkRunsThenUpToDate();
	}

	@Test
	void integrationWithReorderImports() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat('" + NON_DEFAULT_VERSION + "').aosp().reorderImports(true)",
				"    }",
				"}");

		setFile("test.java").toResource("java/googlejavaformat/JavaWithReorderImportsUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaWithReorderImportsEnabledFormatted.test");

		checkRunsThenUpToDate();
		replace("build.gradle",
				"googleJavaFormat('" + NON_DEFAULT_VERSION + "')",
				"googleJavaFormat()");
		checkRunsThenUpToDate();
	}

	@Test
	void integrationWithSkipJavadocFormatting() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"spotless {",
				"    java {",
				"        target file('test.java')",
				"        googleJavaFormat('" + NON_DEFAULT_VERSION + "').skipJavadocFormatting()",
				"    }",
				"}");

		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormattedSkipJavadocFormatting.test");

		checkRunsThenUpToDate();
		replace("build.gradle",
				"googleJavaFormat('" + NON_DEFAULT_VERSION + "')",
				"googleJavaFormat()");
		checkRunsThenUpToDate();
	}
}
