/*
 * Copyright 2022-2023 DiffPlug
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
package com.diffplug.spotless.maven.test;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class CleanthatJavaRefactorerTest extends MavenIntegrationHarness {
	private static final Logger LOGGER = LoggerFactory.getLogger(CleanthatJavaRefactorerTest.class);

	@Test
	void testEnableDraft() throws Exception {
		writePomWithJavaSteps(
				"<cleanthat>",
				"  <sourceJdk>11</sourceJdk>",
				"  <includeDraft>true</includeDraft>",
				"</cleanthat>");

		runTest("MultipleMutators.dirty.test", "MultipleMutators.clean.onlyOptionalIsPresent.test");
	}

	@Test
	void testLiteralsFirstInComparisons() throws Exception {
		writePomWithJavaSteps(
				"<cleanthat>",
				"  <mutators>",
				"    <mutator>LiteralsFirstInComparisons</mutator>",
				"  </mutators>",
				"</cleanthat>");

		runTest("LiteralsFirstInComparisons.dirty.test", "LiteralsFirstInComparisons.clean.test");
	}

	@Test
	void testMultipleMutators_defaultIsJdk7() throws Exception {
		// OptionalNotEmpty will be excluded as it is not compatible with JDK7
		writePomWithJavaSteps(
				"<cleanthat>",
				"  <mutators>",
				"    <mutator>LiteralsFirstInComparisons</mutator>",
				"    <mutator>OptionalNotEmpty</mutator>",
				"  </mutators>",
				"</cleanthat>");

		runTest("MultipleMutators.dirty.test", "MultipleMutators.clean.onlyLiteralsFirst.test");
	}

	@Test
	void testMultipleMutators_Jdk11IntroducedOptionalisPresent() throws Exception {
		writePomWithJavaSteps(
				"<cleanthat>",
				"  <sourceJdk>11</sourceJdk>",
				"  <mutators>",
				"    <mutator>LiteralsFirstInComparisons</mutator>",
				"    <mutator>OptionalNotEmpty</mutator>",
				"  </mutators>",
				"</cleanthat>");

		runTest("MultipleMutators.dirty.test", "MultipleMutators.clean.test");
	}

	@Test
	void testExcludeOptionalNotEmpty() throws Exception {
		writePomWithJavaSteps(
				"<cleanthat>",
				"  <mutators>",
				"    <mutator>LiteralsFirstInComparisons</mutator>",
				"    <mutator>OptionalNotEmpty</mutator>",
				"  </mutators>",
				"  <excludedMutators>",
				"    <excludedMutator>OptionalNotEmpty</excludedMutator>",
				"  </excludedMutators>",
				"</cleanthat>");

		runTest("MultipleMutators.dirty.test", "MultipleMutators.clean.onlyLiteralsFirst.test");
	}

	@Test
	void testIncludeOnlyLiteralsFirstInComparisons() throws Exception {
		writePomWithJavaSteps(
				"<cleanthat>",
				"  <mutators>",
				"    <mutator>LiteralsFirstInComparisons</mutator>",
				"  </mutators>",
				"</cleanthat>");

		runTest("MultipleMutators.dirty.test", "MultipleMutators.clean.onlyLiteralsFirst.test");
	}

	private void runTest(String dirtyPath, String cleanPath) throws Exception {
		var path = "src/main/java/test.java";
		setFile(path).toResource("java/cleanthat/" + dirtyPath);
		// .withRemoteDebug(21654)
		Assertions.assertThat(mavenRunner().withArguments("spotless:apply").runNoError().stdOutUtf8()).doesNotContain("[ERROR]");
		assertFile(path).sameAsResource("java/cleanthat/" + cleanPath);
	}
}
