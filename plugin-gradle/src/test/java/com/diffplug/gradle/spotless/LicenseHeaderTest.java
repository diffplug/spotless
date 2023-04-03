/*
 * Copyright 2016-2023 DiffPlug
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
import java.time.YearMonth;

import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;

class LicenseHeaderTest extends GradleIntegrationHarness {
	private static final String NOW = String.valueOf(YearMonth.now().getYear());

	private static final String TEST_JAVA = "src/main/java/pkg/Test.java";
	private static final String CONTENT = "package pkg;\npublic class Test {}";

	private void setLicenseStep(String licenseLine) throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"  id 'com.diffplug.spotless'",
				"  id 'java'",
				"}",
				"spotless {",
				"  java {",
				licenseLine,
				"  }",
				"}");
	}

	private void assertUnchanged(String year) throws IOException {
		assertTransform(year, year);
	}

	private void assertTransform(String yearBefore, String yearAfter) throws IOException {
		setFile(TEST_JAVA).toContent("/** " + yearBefore + " */\n" + CONTENT);
		gradleRunner().withArguments("spotlessApply", "--stacktrace").forwardOutput().build();
		assertFile(TEST_JAVA).hasContent("/** " + yearAfter + " */\n" + CONTENT);
	}

	private void testSuiteUpdateWithLatest(boolean update) throws IOException {
		if (update) {
			assertTransform("2003", "2003-" + NOW);
			assertTransform("2003-2005", "2003-" + NOW);
		} else {
			assertUnchanged("2003");
			assertUnchanged("2003-2005");
		}
		assertUnchanged(NOW);
		assertTransform("", NOW);
	}

	@Test
	void normal() throws IOException {
		setLicenseStep("licenseHeader('/** $YEAR */')");
		testSuiteUpdateWithLatest(false);
	}

	@Test
	void updateYearWithLatestTrue() throws IOException {
		setLicenseStep("licenseHeader('/** $YEAR */').updateYearWithLatest(true)");
		testSuiteUpdateWithLatest(true);
	}

	@Test
	void filterByContentPatternTest() throws IOException {
		setLicenseStep("licenseHeader('/** $YEAR */').onlyIfContentMatches('.+Test.+').updateYearWithLatest(true)");
		testSuiteUpdateWithLatest(true);
		setLicenseStep("licenseHeader('/** $YEAR */').onlyIfContentMatches('missingString').updateYearWithLatest(true)");
		setFile(TEST_JAVA).toContent("/** This license header should be preserved */\n" + CONTENT);
		gradleRunner().withArguments("spotlessApply", "--stacktrace").forwardOutput().build();
		assertFile(TEST_JAVA).hasContent("/** This license header should be preserved */\n" + CONTENT);
		setLicenseStep("licenseHeader('/** New License Header */').named('PrimaryHeaderLicense').onlyIfContentMatches('.+Test.+')");
		setFile(TEST_JAVA).toContent(CONTENT);
		gradleRunner().withArguments("spotlessApply", "--stacktrace").forwardOutput().build();
		assertFile(TEST_JAVA).hasContent("/** New License Header */\n" + CONTENT);
		var multipleLicenseHeaderConfiguration = "licenseHeader('/** Base License Header */').named('PrimaryHeaderLicense').onlyIfContentMatches('Best')\n" +
				"licenseHeader('/** Alternate License Header */').named('SecondaryHeaderLicense').onlyIfContentMatches('.*Test.+')";
		setLicenseStep(multipleLicenseHeaderConfiguration);
		setFile(TEST_JAVA).toContent("/** 2003 */\n" + CONTENT);
		gradleRunner().withArguments("spotlessApply", "--stacktrace").forwardOutput().build();
		assertFile(TEST_JAVA).hasContent("/** Alternate License Header */\n" + CONTENT);
	}

	@Test
	void ratchetFrom() throws Exception {
		try (Git git = Git.init().setDirectory(rootFolder()).call()) {
			git.commit().setMessage("First commit").call();
		}
		setLicenseStep("licenseHeader('/** $YEAR */')\nratchetFrom 'HEAD'");
		testSuiteUpdateWithLatest(true);
	}

	@Test
	void ratchetFromButUpdateFalse() throws Exception {
		try (Git git = Git.init().setDirectory(rootFolder()).call()) {
			git.commit().setMessage("First commit").call();
		}
		Git.init().setDirectory(rootFolder()).call();
		setLicenseStep("licenseHeader('/** $YEAR */').updateYearWithLatest(false)\nratchetFrom 'HEAD'");
		testSuiteUpdateWithLatest(false);
	}
}
