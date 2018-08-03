/*
 * Copyright 2016 DiffPlug
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import com.diffplug.spotless.LineEnding;

/** Tests the desired behavior from https://github.com/diffplug/spotless/issues/46. */
public class ErrorShouldRethrow extends GradleIntegrationTest {
	private void writeBuild(String... toInsert) throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add("plugins {");
		lines.add("    id 'com.diffplug.gradle.spotless'");
		lines.add("    id 'java'");
		lines.add("}");
		lines.add("spotless {");
		lines.add("    format 'misc', {");
		lines.add("        lineEndings 'UNIX'");
		lines.add("        target file('README.md')");
		lines.add("        custom 'no swearing', {");
		lines.add("             if (it.toLowerCase(Locale.ROOT).contains('fubar')) {");
		lines.add("                 throw new RuntimeException('No swearing!');");
		lines.add("             }");
		lines.add("        }");
		lines.addAll(Arrays.asList(toInsert));
		setFile("build.gradle").toContent(String.join("\n", lines));
	}

	@Test
	public void passesIfNoException() throws Exception {
		writeBuild(
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fun.");
		runWithSuccess(":spotlessMisc");
	}

	@Test
	public void anyExceptionShouldFail() throws Exception {
		writeBuild(
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		runWithFailure(
				":spotlessMisc",
				"Step 'no swearing' found problem in 'README.md'",
				"java.lang.RuntimeException: No swearing!");
	}

	@Test
	public void unlessEnforceCheckIsFalse() throws Exception {
		writeBuild(
				"    } // format",
				"    enforceCheck false",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		runWithSuccess(":compileJava NO-SOURCE");
	}

	@Test
	public void unlessExemptedByStep() throws Exception {
		writeBuild(
				"        ignoreErrorForStep 'no swearing'",
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		runWithSuccess(":spotlessMisc",
				"Unable to apply step 'no swearing' to 'README.md'");
	}

	@Test
	public void unlessExemptedByPath() throws Exception {
		writeBuild(
				"        ignoreErrorForPath 'README.md'",
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		runWithSuccess(":spotlessMisc",
				"Unable to apply step 'no swearing' to 'README.md'");
	}

	@Test
	public void failsIfNeitherStepNorFileExempted() throws Exception {
		writeBuild(
				"        ignoreErrorForStep 'nope'",
				"        ignoreErrorForPath 'nope'",
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		runWithFailure(
				":spotlessMisc",
				"Step 'no swearing' found problem in 'README.md'",
				"java.lang.RuntimeException: No swearing!");
	}

	private void runWithSuccess(String... messagePartsInOrder) throws Exception {
		BuildResult result = gradleRunner().withArguments("check").build();
		assertResultAndOutputContainsMessages(result, TaskOutcome.SUCCESS, messagePartsInOrder);
	}

	private void runWithFailure(String... messagePartsInOrder) throws Exception {
		BuildResult result = gradleRunner().withArguments("check").buildAndFail();
		assertResultAndOutputContainsMessages(result, TaskOutcome.FAILED, messagePartsInOrder);
	}

	private void assertResultAndOutputContainsMessages(BuildResult result, TaskOutcome outcome, String... messagePartsInOrder) {
		String actualOutput = LineEnding.toUnix(result.getOutput());
		assertThat(actualOutput).containsSubsequence(messagePartsInOrder);
		assertThat(result.tasks(outcome).size() + result.tasks(TaskOutcome.UP_TO_DATE).size() + result.tasks(TaskOutcome.NO_SOURCE).size())
				.isEqualTo(result.getTasks().size());
	}
}
