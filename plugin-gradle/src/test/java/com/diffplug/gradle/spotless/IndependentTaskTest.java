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

import java.io.IOException;

import org.junit.jupiter.api.Test;

class IndependentTaskTest extends GradleIntegrationHarness {
	@Test
	void independent() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"repositories { mavenCentral() }",
				"",
				"import com.diffplug.gradle.spotless.JavaExtension",
				"",
				"def underTest = new JavaExtension(spotless)",
				"underTest.target file('test.java')",
				"underTest.googleJavaFormat('1.2')",
				"",
				"def independent = underTest.createIndependentApplyTask('independent')");
		setFile("test.java").toResource("java/googlejavaformat/JavaCodeUnformatted.test");
		gradleRunner().withArguments("independent").build();
		assertFile("test.java").sameAsResource("java/googlejavaformat/JavaCodeFormatted.test");
	}
}
