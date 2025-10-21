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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;

class UpToDateTest extends GradleIntegrationHarness {
	/** Requires that README be lowercase. */
	private void writeBuildFile() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target file('README.md')",
				"        custom 'lowercase', { str -> str.toLowerCase(Locale.ROOT) }",
				"        bumpThisNumberIfACustomStepChanges(1)",
				"    }",
				"}");
	}

	@Test
	void testNormalCase() throws IOException {
		writeBuildFile();
		setFile("README.md").toContent("ABC");
		// first time, the task runs as expected
		applyIsUpToDate(false);
		assertFile("README.md").hasContent("abc");
		// because a file was changed (by spotless),
		// up-to-date is false, even though nothing is
		// going to change during this run.  This second
		// run is very fast though, because it will
		// only run on the few files that were changed.
		applyIsUpToDate(false);
		// it's not until the third run that everything
		// is totally up-to-date
		applyIsUpToDate(true);
	}

	@Test
	void testNearPathologicalCase() throws IOException {
		writeBuildFile();
		setFile("README.md").toContent("ABC");
		// first time, up-to-date is false
		applyIsUpToDate(false);
		assertFile("README.md").hasContent("abc");

		// now we'll change the file
		setFile("README.md").toContent("AB");
		// as expected, the task will run again
		applyIsUpToDate(false);
		assertFile("README.md").hasContent("ab");
		// and it'll take two more runs to get to up-to-date
		applyIsUpToDate(false);
		applyIsUpToDate(true);
	}

	@Test
	void testPathologicalCase() throws IOException {
		writeBuildFile();
		setFile("README.md").toContent("ABC");
		// first time running apply, no tasks are UP-TO-DATE
		applyIsUpToDate(false);
		assertFile("README.md").hasContent("abc");

		// now we'll change the file back to EXACTLY its original content
		setFile("README.md").toContent("ABC");

		// the format task is UP-TO-DATE (same inputs), but the apply tasks will run again
		pauseForFilesystem();
		BuildResult buildResult = gradleRunner().withArguments("spotlessApply").build();
		assertThat(buildResult.taskPaths(TaskOutcome.UP_TO_DATE)).containsExactly(":spotlessInternalRegisterDependencies", ":spotlessMisc");
		assertThat(buildResult.taskPaths(TaskOutcome.SUCCESS)).containsExactly(":spotlessMiscApply", ":spotlessApply");
		assertFile("README.md").hasContent("abc");

		// and it'll take two more runs to get to fully UP-TO-DATE
		applyIsUpToDate(false);
		applyIsUpToDate(true);
	}

	@Test
	void checkAndApply() throws IOException {
		writeBuildFile();
		setFile("README.md").toContent("ABC");
		gradleRunner().withArguments("spotlessCheck", "spotlessApply").build();
	}
}
