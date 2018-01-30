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

import org.junit.Test;

public class SpotlessCheckMojoTest extends MavenIntegrationTest {

	private static final String UNFORMATTED_FILE = "license/MissingLicense.test";
	private static final String FORMATTED_FILE = "license/HasLicense.test";

	@Test
	public void testSpotlessCheckWithFormattingViolations() throws Exception {
		testSpotlessCheck(UNFORMATTED_FILE, null, true);
	}

	@Test
	public void testSpotlessCheckWithoutFormattingViolations() throws Exception {
		testSpotlessCheck(FORMATTED_FILE, null, false);
	}

	@Test
	public void testSkipSpotlessCheckWithFormattingViolations() throws Exception {
		testSpotlessCheck(UNFORMATTED_FILE, "-Dspotless.check.skip", false);
	}

	private void testSpotlessCheck(String fileName, String additionalMvnArg, boolean expectError) throws Exception {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>");

		write("license.txt", getTestResource("license/TestLicense"));
		write("src/main/java/test.java", getTestResource(fileName));

		MavenRunner mavenRunner = mavenRunner();
		if (additionalMvnArg == null) {
			mavenRunner.withArguments("spotless:check");
		} else {
			mavenRunner.withArguments("spotless:check", additionalMvnArg);
		}

		if (expectError) {
			mavenRunner.runHasError();
		} else {
			mavenRunner.runNoError();
		}
	}
}
