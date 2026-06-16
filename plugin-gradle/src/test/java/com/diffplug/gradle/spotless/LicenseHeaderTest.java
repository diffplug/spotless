/*
 * Copyright 2016-2026 DiffPlug
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

import com.diffplug.spotless.ClearGitConfig;

@ClearGitConfig
class LicenseHeaderTest extends GradleIntegrationHarness {
	private static final String NOW = String.valueOf(YearMonth.now().getYear());

	private static final String TEST_JAVA = "src/main/java/pkg/Test.java";
	private static final String CONTENT = "package pkg;\npublic class Test {}";

	private void setLicenseStep(String licenseLine) {
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

	private String formatYearStr(String yearFmt, String year) {
		if (yearFmt == null) {
			yearFmt = "%s";
		}
		return yearFmt.formatted(year);
	}

	private void assertUnchanged(String yearFmt, String year) throws IOException {
		assertTransform(yearFmt, year, year);
	}

	private void assertTransform(String yearFmt, String yearBefore, String yearAfter) throws IOException {
		final String yearAfterStr = formatYearStr(yearFmt, yearAfter);

		setFile(TEST_JAVA).toContent("/** " + yearBefore + " */\n" + CONTENT);
		gradleRunner().withArguments("spotlessApply", "--stacktrace").forwardOutput().build();
		assertFile(TEST_JAVA).hasContent("/** " + yearAfterStr + " */\n" + CONTENT);
	}

	private void testSuiteUpdateWithLatest(boolean update) throws IOException {
		testSuiteUpdateWithLatest(update, null);
	}

	private void testSuiteUpdateWithLatest(boolean update, String yearFmt) throws IOException {
		if (update) {
			assertTransform(yearFmt, "2003", "2003-" + NOW);
			assertTransform(yearFmt, "   2003", "2003-" + NOW);
			assertTransform(yearFmt, "2003   ", "2003-" + NOW);
			assertTransform(yearFmt, "   2003   ", "2003-" + NOW);

			assertTransform(yearFmt, "2003-2005", "2003-" + NOW);
			assertTransform(yearFmt, "   2003-2005", "2003-" + NOW);
			assertTransform(yearFmt, "2003-2005   ", "2003-" + NOW);
			assertTransform(yearFmt, "   2003-2005   ", "2003-" + NOW);
		} else {
			assertUnchanged(yearFmt, "2003");
			assertTransform(yearFmt, "   2003", "2003");
			assertTransform(yearFmt, "2003   ", "2003");
			assertTransform(yearFmt, "   2003   ", "2003");

			assertUnchanged(yearFmt, "2003-2005");
			assertTransform(yearFmt, "   2003-2005", "2003-2005");
			assertTransform(yearFmt, "2003-2005   ", "2003-2005");
			assertTransform(yearFmt, "   2003-2005   ", "2003-2005");
		}
		assertUnchanged(yearFmt, NOW);
		assertTransform(yearFmt, "   " + NOW, NOW);
		assertTransform(yearFmt, NOW + "   ", NOW);
		assertTransform(yearFmt, "   " + NOW + "   ", NOW);

		assertTransform(yearFmt, "", NOW);
		assertTransform(yearFmt, "   ", NOW);
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
	void withYearStringFormat() throws IOException {
		// default format
		setLicenseStep("licenseHeader('/** $YEAR */').yearStringFormat('%s')");
		testSuiteUpdateWithLatest(false, "%s");

		// fill with spaces before
		setLicenseStep("licenseHeader('/** $YEAR */').yearStringFormat('%9s')");
		testSuiteUpdateWithLatest(false, "%9s");

		// fill with spaces after
		setLicenseStep("licenseHeader('/** $YEAR */').yearStringFormat('%-12s')");
		testSuiteUpdateWithLatest(false, "%-12s");
	}

	@Test
	void updateYearWithLatestTrue_withYearStringFormat() throws IOException {
		// default format
		setLicenseStep("licenseHeader('/** $YEAR */').updateYearWithLatest(true).yearStringFormat('%s')");
		testSuiteUpdateWithLatest(true, "%s");

		// fill with spaces before
		setLicenseStep("licenseHeader('/** $YEAR */').updateYearWithLatest(true).yearStringFormat('%10s')");
		testSuiteUpdateWithLatest(true, "%10s");

		// fill with spaces after
		setLicenseStep("licenseHeader('/** $YEAR */').updateYearWithLatest(true).yearStringFormat('%-15s')");
		testSuiteUpdateWithLatest(true, "%-15s");
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
		String multipleLicenseHeaderConfiguration = """
				licenseHeader('/** Base License Header */').named('PrimaryHeaderLicense').onlyIfContentMatches('Best')
				licenseHeader('/** Alternate License Header */').named('SecondaryHeaderLicense').onlyIfContentMatches('.*Test.+')""";
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
