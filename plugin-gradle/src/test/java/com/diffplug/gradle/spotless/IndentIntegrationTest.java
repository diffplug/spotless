/*
 * Copyright 2021-2025 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.stream.Stream;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class IndentIntegrationTest extends GradleIntegrationHarness {

	@ParameterizedTest
	@ValueSource(strings = {"indentWithSpaces", "indentWithTabs"})
	void oldIndentApiLogsDeprecationWarning(String indentationMethodName) throws IOException {
		BuildResult result = runIndentFormatter(indentationMethodName);
		assertThat(result.getOutput()).containsPattern(".*" + indentationMethodName + ".*deprecated.*");
	}

	@ParameterizedTest
	@ValueSource(strings = {"leadingTabsToSpaces", "leadingSpacesToTabs"})
	void newIndentApiDoesNotLogDeprecationWarning(String indentationMethodName) throws IOException {
		BuildResult result = runIndentFormatter(indentationMethodName);
		assertThat(result.getOutput()).doesNotContainPattern(".*" + indentationMethodName + ".*deprecated.*");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("indentationCombinations")
	void indentationCombinations(String testName, String indentationMethodName, String actualResource, String expectedResultResource) throws IOException {
		runIndentFormatter(indentationMethodName, actualResource);
		assertFile("test.txt").sameAsResource(expectedResultResource);
	}

	private static Stream<Arguments> indentationCombinations() {
		return Stream.of(
				// new API
				Arguments.of("tabsToTabs", "leadingSpacesToTabs", "indent/IndentedWithTab.test", "indent/IndentedWithTab.test"),
				Arguments.of("spacesToSpaces", "leadingTabsToSpaces", "indent/IndentedWithSpace.test", "indent/IndentedWithSpace.test"),
				Arguments.of("spacesToTabs", "leadingSpacesToTabs", "indent/IndentedWithSpace.test", "indent/IndentedWithTab.test"),
				Arguments.of("tabsToSpaces", "leadingTabsToSpaces", "indent/IndentedWithTab.test", "indent/IndentedWithSpace.test"),
				Arguments.of("mixedToTabs", "leadingSpacesToTabs", "indent/IndentedMixed.test", "indent/IndentedWithTab.test"),
				Arguments.of("mixedToSpaces", "leadingTabsToSpaces", "indent/IndentedMixed.test", "indent/IndentedWithSpace.test"),
				// legacy API
				Arguments.of("legacy: tabsToTabs", "indentWithTabs", "indent/IndentedWithTab.test", "indent/IndentedWithTab.test"),
				Arguments.of("legacy: spacesToSpaces", "indentWithSpaces", "indent/IndentedWithSpace.test", "indent/IndentedWithSpace.test"),
				Arguments.of("legacy: spacesToTabs", "indentWithTabs", "indent/IndentedWithSpace.test", "indent/IndentedWithTab.test"),
				Arguments.of("legacy: tabsToSpaces", "indentWithSpaces", "indent/IndentedWithTab.test", "indent/IndentedWithSpace.test"),
				Arguments.of("legacy: mixedToTabs", "indentWithTabs", "indent/IndentedMixed.test", "indent/IndentedWithTab.test"),
				Arguments.of("legacy: mixedToSpaces", "indentWithSpaces", "indent/IndentedMixed.test", "indent/IndentedWithSpace.test"));
	}

	private BuildResult runIndentFormatter(String indentationMethodName) throws IOException {
		return runIndentFormatter(indentationMethodName, "indent/IndentedMixed.test");
	}

	private BuildResult runIndentFormatter(String indentationMethodName, String resourceFile) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"  format 'test', {",
				"    target '**/*.txt'",
				"    " + indentationMethodName + "()",
				"  }",
				"}");
		setFile("test.txt").toResource(resourceFile);
		return gradleRunner().withArguments("spotlessApply").build();
	}

}
