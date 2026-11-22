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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShowAllFailuresTest extends GradleIntegrationHarness {
	private static final int FILE_COUNT = 30;

	@BeforeEach
	void createProjectWithManyBadFiles() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target '*.txt'",
				"        leadingTabsToSpaces(2)",
				"    }",
				"}");
		for (int i = 0; i < FILE_COUNT; i++) {
			setFile("%02d.txt".formatted(i)).toContent("\thello\n");
		}
	}

	@Test
	void summaryByDefault() throws IOException {
		String output = gradleRunner().withArguments("spotlessCheck").buildAndFail().getOutput();
		Assertions.assertThat(output)
				.contains(" other files.")
				.doesNotContain("    %02d.txt".formatted(FILE_COUNT - 1));
	}

	@Test
	void listsEveryFileWithProperty() throws IOException {
		String output = gradleRunner().withArguments("spotlessCheck", "-PspotlessShowAllFailures").buildAndFail().getOutput();
		Assertions.assertThat(output)
				.doesNotContain(" other files.")
				.contains("Violations also present in:");
		for (int i = 0; i < FILE_COUNT; i++) {
			Assertions.assertThat(output).contains("    %02d.txt".formatted(i));
		}
	}

	@Test
	void listsEveryFileWithGradleProperties() throws IOException {
		setFile("gradle.properties").toContent("spotlessShowAllFailures=true");
		String output = gradleRunner().withArguments("spotlessCheck").buildAndFail().getOutput();
		Assertions.assertThat(output)
				.doesNotContain(" other files.")
				.contains("    %02d.txt".formatted(FILE_COUNT - 1));
	}

	@Test
	void summaryWhenPropertyIsFalse() throws IOException {
		String output = gradleRunner().withArguments("spotlessCheck", "-PspotlessShowAllFailures=false").buildAndFail().getOutput();
		Assertions.assertThat(output).contains(" other files.");
	}
}
