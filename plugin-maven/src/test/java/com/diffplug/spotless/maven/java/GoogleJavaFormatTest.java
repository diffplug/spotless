/*
 * Copyright 2016-2025 DiffPlug
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

class GoogleJavaFormatTest extends MavenIntegrationHarness {
	@Test
	void specificVersionDefaultStyle() throws Exception {
		writePomWithJavaSteps(
				"<googleJavaFormat>",
				"  <version>1.17.0</version>",
				"</googleJavaFormat>");

		runTest("java/googlejavaformat/JavaCodeFormatted.test");
	}

	@Test
	void specificVersionSpecificStyle() throws Exception {
		writePomWithJavaSteps(
				"<googleJavaFormat>",
				"  <version>1.17.0</version>",
				"  <style>AOSP</style>",
				"</googleJavaFormat>");

		runTest("java/googlejavaformat/JavaCodeFormattedAOSP.test");
	}

	@Test
	void specificVersionReflowLongStrings() throws Exception {
		writePomWithJavaSteps(
				"<googleJavaFormat>",
				"  <version>1.17.0</version>",
				"  <reflowLongStrings>true</reflowLongStrings>",
				"</googleJavaFormat>");

		runTest("java/googlejavaformat/JavaCodeFormattedReflowLongStrings.test");
	}

	@Test
	void specificVersionReorderImports() throws Exception {
		writePomWithJavaSteps(
				"<googleJavaFormat>",
				"  <version>1.17.0</version>",
				"  <style>AOSP</style>",
				"  <reorderImports>true</reorderImports>",
				"</googleJavaFormat>");

		runTest("java/googlejavaformat/JavaWithReorderImportsEnabledFormatted.test", "java/googlejavaformat/JavaWithReorderImportsUnformatted.test");
	}

	@Test
	void specificVersionSkipJavadocFormatting() throws Exception {
		writePomWithJavaSteps(
				"<googleJavaFormat>",
				"  <version>1.17.0</version>",
				"  <formatJavadoc>false</formatJavadoc>",
				"</googleJavaFormat>");

		runTest("java/googlejavaformat/JavaCodeFormattedSkipJavadocFormatting.test");
	}

	private void runTest(String targetResource) throws Exception {
		runTest(targetResource, "java/googlejavaformat/JavaCodeUnformatted.test");
	}

	private void runTest(String targetResource, String sourceResource) throws Exception {
		setFile(testPath).toResource(sourceResource);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(testPath).sameAsResource(targetResource);
	}
}
