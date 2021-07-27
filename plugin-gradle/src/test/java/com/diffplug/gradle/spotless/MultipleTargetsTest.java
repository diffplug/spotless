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
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import com.diffplug.common.collect.Lists;

class MultipleTargetsTest extends GradleIntegrationHarness {
	private static final List<String> TARGET_FILES = Lists.newArrayList("src/test.md", "src/test.1.txt", "src/test.2.txt");
	private static final List<String> NON_TARGET_FILES = Lists.newArrayList(".git/test.1.txt", ".gradle/test.1.txt", "build/test.1.txt");

	@Test
	void testSingleRecursiveTargetMatchingSuffix() throws IOException {
		runTest("target '**/*.txt'");
	}

	@Test
	void testSingleRecursiveTargetMatchingPrefix() throws IOException {
		runTest("target '**/test.*'");
	}

	@Test
	void testSingleRecursiveTargetMatchingSubdirectory() throws IOException {
		runTest("target 'src/**/*.txt'");
	}

	@Test
	void testExplicitTargets() throws IOException {
		runTest("target('src/test.1.txt', 'src/test.2.txt')");
	}

	@Test
	void testNonRecursiveTargets() throws IOException {
		runTest("target('src/*.1.txt', 'src/*.2.txt')");
	}

	@Test
	void testTwoRecursiveTargetsMatchingDifferentFiles() throws IOException {
		runTest("target('**/*.1.txt', '**/*.2.txt')");
	}

	@Test
	void testTwoRecursiveTargetsMatchingIdenticalFiles() throws IOException {
		runTest("target('**/*.txt', '**/test.*')");
	}

	@Test
	void testTwoRecursiveTargetsProvidedAsList() throws IOException {
		runTest("target(['**/*.1.txt', '**/*.2.txt'])");
	}

	private void runTest(String targets) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format 'singleFile', {",
				"        target 'src/test.md'",
				"        custom 'lowercase', { it.toLowerCase(Locale.ROOT) }",
				"        bumpThisNumberIfACustomStepChanges(1)",
				"    }",
				"    format 'multipleFiles', {",
				"        " + targets,
				"        custom 'lowercase', { it.toLowerCase(Locale.ROOT) }",
				"        bumpThisNumberIfACustomStepChanges(1)",
				"    }",
				"}");

		initContent(TARGET_FILES, "A");
		initContent(NON_TARGET_FILES, "A");

		BuildResult result = gradleRunner().withArguments("spotlessApply").build();

		checkContent(TARGET_FILES, "a");
		checkContent(NON_TARGET_FILES, "A");
	}

	private void initContent(List<String> files, String content) throws IOException {
		for (String file : files) {
			setFile(file).toContent(content);
		}
	}

	private void checkContent(List<String> files, String content) throws IOException {
		for (String file : files) {
			assertFile(file).hasContent(content);
		}
	}
}
