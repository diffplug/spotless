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
package com.diffplug.spotless.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner;

class SpotlessCheckMojoTest extends MavenIntegrationHarness {

	private static final String UNFORMATTED_FILE = "license/MissingLicense.test";
	private static final String FORMATTED_FILE = "license/HasLicense.test";
	private static final String TARGET_JAVA = "src/main/java/com.github.youribonnaffe.gradle.format/Java8Test.java";

	@Test
	void testSpotlessCheckWithFormattingViolations() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		testSpotlessCheck(UNFORMATTED_FILE, "spotless:check", true);
	}

	@Test
	void testSpotlessCheckWithoutFormattingViolations() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		testSpotlessCheck(FORMATTED_FILE, "spotless:check", false);
	}

	@Test
	void testSkipSpotlessCheckWithFormattingViolations() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		testSpotlessCheck(UNFORMATTED_FILE, "spotless:check -Dspotless.check.skip", false);
	}

	@Test
	void testSkipAllGoalsWithSpotlessSkip() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		testSpotlessCheck(UNFORMATTED_FILE, "spotless:check -Dspotless.skip", false);
	}

	@Test
	void testApplySkipDoesNotSkipCheck() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		// apply.skip must not suppress check
		testSpotlessCheck(UNFORMATTED_FILE, "spotless:check -Dspotless.apply.skip", true);
	}

	@Test
	void testSpotlessCheckBindingToVerifyPhase() throws Exception {
		writePom(
				new String[]{
						"<execution>",
						"  <id>check</id>",
						"  <goals>",
						"    <goal>check</goal>",
						"  </goals>",
						"</execution>"},
				new String[]{
						"<java>",
						"  <licenseHeader>",
						"    <file>${basedir}/license.txt</file>",
						"  </licenseHeader>",
						"</java>"},
				null,
				null);

		testSpotlessCheck(UNFORMATTED_FILE, "verify", true);
	}

	@Test
	void testApplySkipLeavesFileUnformatted() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		setFile("license.txt").toResource("license/TestLicense");
		setFile(TARGET_JAVA).toResource(UNFORMATTED_FILE);

		mavenRunner().withArguments("spotless:apply -Dspotless.apply.skip").runNoError();
		assertFile(TARGET_JAVA).sameAsResource(UNFORMATTED_FILE);
	}

	@Test
	void testSpotlessSkipLeavesApplyUnformatted() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		setFile("license.txt").toResource("license/TestLicense");
		setFile(TARGET_JAVA).toResource(UNFORMATTED_FILE);

		mavenRunner().withArguments("spotless:apply -Dspotless.skip").runNoError();
		assertFile(TARGET_JAVA).sameAsResource(UNFORMATTED_FILE);
	}

	@Test
	void testCheckSkipDoesNotSkipApply() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		setFile("license.txt").toResource("license/TestLicense");
		setFile(TARGET_JAVA).toResource(UNFORMATTED_FILE);

		mavenRunner().withArguments("spotless:apply -Dspotless.check.skip").runNoError();
		assertFile(TARGET_JAVA).sameAsResource(FORMATTED_FILE);
	}

	private void testSpotlessCheck(String fileName, String command, boolean expectError) throws Exception {
		setFile("license.txt").toResource("license/TestLicense");
		setFile(TARGET_JAVA).toResource(fileName);

		MavenRunner mavenRunner = mavenRunner().withArguments(command);

		if (expectError) {
			ProcessRunner.Result result = mavenRunner.runHasError();
			assertThat(result.stdOutUtf8()).contains("The following files had format violations");
		} else {
			mavenRunner.runNoError();
		}
	}

	private void writePomWithJavaLicenseHeaderStep() throws IOException {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>");
	}
}
