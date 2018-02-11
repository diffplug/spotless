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
package com.diffplug.spotless.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

public class SpotlessCheckMojoTest extends MavenIntegrationTest {

	private static final String UNFORMATTED_FILE = "license/MissingLicense.test";
	private static final String FORMATTED_FILE = "license/HasLicense.test";

	@Test
	public void testSpotlessCheckWithFormattingViolations() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		testSpotlessCheck(UNFORMATTED_FILE, "spotless:check", true);
	}

	@Test
	public void testSpotlessCheckWithoutFormattingViolations() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		testSpotlessCheck(FORMATTED_FILE, "spotless:check", false);
	}

	@Test
	public void testSkipSpotlessCheckWithFormattingViolations() throws Exception {
		writePomWithJavaLicenseHeaderStep();
		testSpotlessCheck(UNFORMATTED_FILE, "spotless:check -Dspotless.check.skip", false);
	}

	@Test
	public void testSpotlessCheckBindingToVerifyPhase() throws Exception {
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
						"</java>"});

		testSpotlessCheck(UNFORMATTED_FILE, "verify", true);
	}

	private void testSpotlessCheck(String fileName, String command, boolean expectError) throws Exception {
		write("license.txt", getTestResource("license/TestLicense"));
		write("src/main/java/com.github.youribonnaffe.gradle.format/Java8Test.java", getTestResource(fileName));

		MavenRunner mavenRunner = mavenRunner().withArguments(command);

		if (expectError) {
			MavenRunner.Result result = mavenRunner.runHasError();
			assertThat(result.output()).contains("The following files had format violations");
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
