/*
 * Copyright 2022-2025 DiffPlug
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
package com.diffplug.spotless.maven.java;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class PalantirJavaFormatTest extends MavenIntegrationHarness {
	@Test
	void specificVersionDefaultStyle() throws Exception {
		writePomWithJavaSteps(
				"<palantirJavaFormat>",
				"  <version>1.1.0</version>",
				"</palantirJavaFormat>");

		runTest("java/palantirjavaformat/JavaCodeFormatted.test", "java/palantirjavaformat/JavaCodeUnformatted.test");
	}

	@Test
	void specificJava11Version2() throws Exception {
		writePomWithJavaSteps(
				"<palantirJavaFormat>",
				"  <version>2.39.0</version>",
				"</palantirJavaFormat>");

		runTest("java/palantirjavaformat/JavaCodeFormatted.test", "java/palantirjavaformat/JavaCodeUnformatted.test");
	}

	@Test
	void formatJavaDoc() throws Exception {
		writePomWithJavaSteps(
				"<palantirJavaFormat>",
				"  <version>2.39.0</version>",
				"  <formatJavadoc>true</formatJavadoc>",
				"</palantirJavaFormat>");

		runTest("java/palantirjavaformat/JavaCodeWithJavaDocFormatted.test", "java/palantirjavaformat/JavaCodeWithJavaDocUnformatted.test");
	}

	private void runTest(String targetResource, String sourceResource) throws Exception {
		setFile(testPath).toResource(sourceResource);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(testPath).sameAsResource(targetResource);
	}
}
