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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FileSignature;

/**
 * Regression for <a href="https://github.com/diffplug/spotless/issues/2421">#2421</a>:
 * when Spotless runs inside an included build of a composite, the run-to-fix hint must
 * use the build-tree task path (e.g. {@code :my-utils:spotlessApply}), not a bare
 * {@code spotlessApply} which only selects tasks in the root/main build.
 */
class CompositeBuildRunToFixMessageTest extends GradleIntegrationHarness {

	private static String expectedGradleCommand() {
		return FileSignature.machineIsWin() ? "gradlew.bat" : "./gradlew";
	}

	@Test
	void includedBuildRootSuggestsBuildTreePath() throws IOException {
		createCompositeWithIncluded("my-utils", null);

		String output = gradleRunner()
				.withArguments(":my-utils:spotlessCheck")
				.buildAndFail()
				.getOutput();

		Assertions.assertThat(output)
				.contains("Run '" + expectedGradleCommand() + " :my-utils:spotlessApply' to fix all violations.");
	}

	@Test
	void includedBuildSubprojectSuggestsBuildTreePath() throws IOException {
		createCompositeWithIncluded("my-utils", "lib");

		String output = gradleRunner()
				.withArguments(":my-utils:lib:spotlessCheck")
				.buildAndFail()
				.getOutput();

		Assertions.assertThat(output)
				.contains("Run '" + expectedGradleCommand() + " :my-utils:lib:spotlessApply' to fix all violations.");
	}

	@Test
	void standaloneBuildStillSuggestsBareSpotlessApply() throws IOException {
		// Outside a composite, bare spotlessApply is correct and preferred (#2592).
		setFile("settings.gradle").toContent("rootProject.name = 'standalone'");
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target 'test.txt'",
				"        leadingTabsToSpaces(2)",
				"    }",
				"}");
		setFile("test.txt").toContent("\thello\n");

		String output = gradleRunner()
				.withArguments("spotlessCheck")
				.buildAndFail()
				.getOutput();

		Assertions.assertThat(output)
				.contains("Run '" + expectedGradleCommand() + " spotlessApply' to fix all violations.")
				.doesNotContain(":spotlessApply");
	}

	private void createCompositeWithIncluded(String includedName, String subproject) throws IOException {
		setFile("settings.gradle").toLines(
				"rootProject.name = 'my-composite'",
				"includeBuild('" + includedName + "')");
		setFile("build.gradle").toContent("");

		setFile(includedName + "/settings.gradle").toContent(
				subproject == null
						? "rootProject.name = '" + includedName + "'"
						: "rootProject.name = '" + includedName + "'\ninclude '" + subproject + "'");

		String spotlessBlock = String.join("\n",
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target 'test.txt'",
				"        leadingTabsToSpaces(2)",
				"    }",
				"}");

		if (subproject == null) {
			setFile(includedName + "/build.gradle").toContent(spotlessBlock);
			setFile(includedName + "/test.txt").toContent("\thello\n");
		} else {
			setFile(includedName + "/build.gradle").toContent("");
			setFile(includedName + "/" + subproject + "/build.gradle").toContent(spotlessBlock);
			setFile(includedName + "/" + subproject + "/test.txt").toContent("\thello\n");
		}
	}
}
