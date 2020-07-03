/*
 * Copyright 2016-2020 DiffPlug
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

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.diffplug.spotless.FileSignature;

@Category(ExcludeFromPluginGradleModern.class)
public class SpecificFilesTest extends GradleIntegrationHarness {

	private static String regexWinSafe(String input) {
		return FileSignature.machineIsWin() ? input.replace("/", "\\\\") : input;
	}

	private String testFilePath(int number) {
		return testFilePath(number, true);
	}

	private String testFilePath(int number, boolean absolute) {
		String relPath = "src/main/java/test" + number + ".java";
		String returnValue;
		if (absolute) {
			returnValue = rootFolder().getAbsolutePath().replace('\\', '/') + "/" + relPath;
		} else {
			returnValue = relPath;
		}
		// regex-escape on windows;
		return regexWinSafe(returnValue);
	}

	private String fixture() {
		return fixture(false);
	}

	private String fixture(boolean formatted) {
		return "java/googlejavaformat/JavaCode" + (formatted ? "F" : "Unf") + "ormatted.test";
	}

	private void createBuildScript() throws IOException {
		createBuildScript(false);
	}

	private void createBuildScript(boolean isKotlin) throws IOException {
		if (isKotlin) {
			setFile("build.gradle.kts").toLines(
					"import com.diffplug.gradle.spotless.SpotlessExtension",
					"buildscript {",
					"    repositories {",
					"        mavenCentral()",
					"    }",
					"    dependencies {",
					"        classpath(\"com.diffplug.spotless:spotless-plugin-gradle:3.27.1\")",
					"    }",
					"}",
					"plugins {",
					"    java",
					"    id(\"com.diffplug.gradle.spotless\")",
					"}",
					"configure<SpotlessExtension> {",
					"    java {",
					"        googleJavaFormat(\"1.2\")",
					"    }",
					"}");
			return;
		}

		setFile("build.gradle").toLines(
				"buildscript { repositories { mavenCentral() } }",
				"plugins {",
				"    id 'com.diffplug.gradle.spotless'",
				"}",
				"apply plugin: 'java'",
				"spotless {",
				"    java {",
				"        googleJavaFormat('1.2')",
				"    }",
				"}");
	}

	private void integration(String patterns,
			boolean firstFormatted, boolean secondFormatted, boolean thirdFormatted) throws IOException {
		integration(patterns, firstFormatted, secondFormatted, thirdFormatted, false);
	}

	private void integration(String patterns,
			boolean firstFormatted, boolean secondFormatted, boolean thirdFormatted,
			boolean isKotlin) throws IOException {
		String testFileOne = testFilePath(1, false);
		String testFileTwo = testFilePath(2, false);
		String testFileThree = testFilePath(3, false);

		setFile(testFileOne).toResource(fixture());
		setFile(testFileTwo).toResource(fixture());
		setFile(testFileThree).toResource(fixture());

		GradleRunner runner = gradleRunner()
				.withArguments("spotlessApply", "-PspotlessFiles=" + patterns);
		if (isKotlin) {
			runner.withGradleVersion(GradleVersionSupport.KOTLIN.version);
		}
		runner.build();

		assertFile(testFileOne).sameAsResource(fixture(firstFormatted));
		assertFile(testFileTwo).sameAsResource(fixture(secondFormatted));
		assertFile(testFileThree).sameAsResource(fixture(thirdFormatted));
	}

	@Test
	public void singleFile() throws IOException {
		createBuildScript(false);
		integration(testFilePath(2), false, true, false);
	}

	@Test
	public void multiFile() throws IOException {
		createBuildScript();
		integration(testFilePath(1) + "," + testFilePath(3),
				true, false, true);
	}

	@Test
	@Ignore("When spotlessFiles is specified without a value, Spotless runs on all files. It should run on none.")
	public void emptyPattern_formatsNoFiles() throws IOException {
		createBuildScript();
		integration("", false, false, false);
	}

	@Test
	public void matchesNoFiles_formatsNoFilesButDoesNotExitInError() throws IOException {
		createBuildScript();
		integration(testFilePath(4), false, false, false);
	}

	@Test
	public void regexp() throws IOException {
		createBuildScript();
		integration(regexWinSafe(".*/src/main/java/test(1|3).java"), true, false, true);
	}

	@Test(expected = UnexpectedBuildFailure.class)
	public void invalidRegexp_exitsInError() throws IOException {
		createBuildScript(false);
		integration("./[?)!\\", false, false, false);
	}

	@Test
	public void kotlinBuildScript() throws IOException {
		createBuildScript(true);
		integration(testFilePath(2), false, true, false, true);
	}
}
