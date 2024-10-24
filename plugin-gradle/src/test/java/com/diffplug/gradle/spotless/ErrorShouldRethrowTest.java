/*
 * Copyright 2016-2024 DiffPlug
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Test;

import com.diffplug.selfie.Selfie;
import com.diffplug.selfie.StringSelfie;

/** Tests the desired behavior from https://github.com/diffplug/spotless/issues/46. */
class ErrorShouldRethrowTest extends GradleIntegrationHarness {
	private void writeBuild(String... toInsert) throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add("plugins {");
		lines.add("    id 'com.diffplug.spotless'");
		lines.add("    id 'java'");
		lines.add("}");
		lines.add("spotless {");
		lines.add("    format 'misc', {");
		lines.add("        lineEndings 'UNIX'");
		lines.add("        target file('README.md')");
		lines.add("        custom 'noSwearingStep', {");
		lines.add("             if (it.toLowerCase(Locale.ROOT).contains('fubar')) {");
		lines.add("                 throw com.diffplug.spotless.Lint.atUndefinedLine('swearing', 'No swearing!').shortcut();");
		lines.add("             }");
		lines.add("        }");
		lines.addAll(Arrays.asList(toInsert));
		setFile("build.gradle").toContent(String.join("\n", lines));
	}

	@Test
	void passesIfNoException() throws Exception {
		writeBuild(
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fun.");
		expectSuccess();
	}

	@Test
	void anyExceptionShouldFail() throws Exception {
		writeBuild(
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		expectFailureAndConsoleToBe().toBe("> Task :spotlessMisc",
				"> Task :spotlessMiscCheck FAILED",
				"",
				"FAILURE: Build failed with an exception.",
				"",
				"* What went wrong:",
				"Execution failed for task ':spotlessMiscCheck'.",
				"> There were 1 lint error(s), they must be fixed or suppressed.",
				"  README.md:LINE_UNDEFINED noSwearingStep(swearing) No swearing!",
				"  Resolve these lints or suppress with `suppressLintsFor`");
	}

	@Test
	void unlessEnforceCheckIsFalse() throws Exception {
		writeBuild(
				"    } // format",
				"    enforceCheck false",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		expectSuccess();
	}

	@Test
	void unlessExemptedByStep() throws Exception {
		writeBuild(
				"        ignoreErrorForStep 'noSwearingStep'",
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		expectSuccess();
	}

	@Test
	void unlessExemptedByPath() throws Exception {
		writeBuild(
				"        ignoreErrorForPath 'README.md'",
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		expectSuccess();
	}

	@Test
	void failsIfNeitherStepNorFileExempted() throws Exception {
		writeBuild(
				"        ignoreErrorForStep 'nope'",
				"        ignoreErrorForPath 'nope'",
				"    } // format",
				"}     // spotless");
		setFile("README.md").toContent("This code is fubar.");
		expectFailureAndConsoleToBe().toBe("> Task :spotlessMisc",
				"> Task :spotlessMiscCheck FAILED",
				"",
				"FAILURE: Build failed with an exception.",
				"",
				"* What went wrong:",
				"Execution failed for task ':spotlessMiscCheck'.",
				"> There were 1 lint error(s), they must be fixed or suppressed.",
				"  README.md:LINE_UNDEFINED noSwearingStep(swearing) No swearing!",
				"  Resolve these lints or suppress with `suppressLintsFor`");
	}

	private void expectSuccess() throws Exception {
		gradleRunner().withArguments("check", "--stacktrace").build();
	}

	private StringSelfie expectFailureAndConsoleToBe() throws Exception {
		BuildResult result = gradleRunner().withArguments("check").buildAndFail();
		String output = result.getOutput();
		int register = output.indexOf(":spotlessInternalRegisterDependencies");
		int firstNewlineAfterThat = output.indexOf('\n', register + 1);
		int firstTry = output.indexOf("\n* Try:");
		String useThisToMatch = output.substring(firstNewlineAfterThat, firstTry).trim();
		return Selfie.expectSelfie(useThisToMatch);
	}
}
