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

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Test;

import com.diffplug.common.base.CharMatcher;
import com.diffplug.common.base.Splitter;
import com.diffplug.common.base.StringPrinter;

/** Tests the desired behavior from https://github.com/diffplug/spotless/issues/46. */
public class ErrorShouldRethrow extends GradleIntegrationTest {
	@Test
	public void noSwearing() throws Exception {
		File build = write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        target file('README.md')",
				"        custom 'no swearing', {",
				"             if (it.toLowerCase(Locale.US).contains('fubar')) {",
				"                 throw new AssertionError('No swearing!');",
				"             }",
				"        }",
				"    }",
				"}");
		write("README.md", "This code is fubar.");
		BuildResult result = gradleRunner().withArguments("spotlessCheck").buildAndFail();

		String expectedToStartWith = StringPrinter.buildStringFromLines(
				":spotlessMiscCheckStep 'no swearing' found problem in 'README.md':",
				"No swearing!",
				" FAILED",
				"",
				"FAILURE: Build failed with an exception.",
				"",
				"* Where:",
				"Build file '" + build + "' line: 9",
				"",
				"* What went wrong:",
				"Execution failed for task ':spotlessMiscCheck'.",
				"> No swearing!");
		int numNewlines = CharMatcher.is('\n').countIn(expectedToStartWith);
		List<String> actualLines = Splitter.on('\n').splitToList(LineEnding.toUnix(result.getOutput()));
		String actualStart = actualLines.subList(0, numNewlines + 1).stream().collect(Collectors.joining("\n"));
		Assertions.assertThat(actualStart).isEqualTo(expectedToStartWith);
		Assertions.assertThat(result.tasks(TaskOutcome.FAILED))
				.isNotEmpty()
				.hasSameSizeAs(result.getTasks());
	}

	@Test
	public void noSwearingPassesIfNoSwears() throws Exception {
		write("build.gradle",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"spotless {",
				"    format 'misc', {",
				"        lineEndings 'UNIX'",
				"        target file('README.md')",
				"        custom 'no swearing', {",
				"             if (it.toLowerCase(Locale.US).contains('fubar')) {",
				"                 throw new AssertionError('No swearing!');",
				"             }",
				"        }",
				"        bumpThisNumberIfACustomRuleChanges(1)",
				"    }",
				"}");
		write("README.md", "This code is fun.");
		checkRunsThenUpToDate();
	}
}
