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
package com.diffplug.spotless.maven.generic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.diffplug.spotless.maven.MavenIntegrationTest;

public class LicenseHeaderTest extends MavenIntegrationTest {
	private static final String KEY_LICENSE = "license/TestLicense";

	@Test
	public void fromFile() throws Exception {
		setFile("license.txt").toResource(KEY_LICENSE);
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>");
		runTest();
	}

	@Test
	public void fromContent() throws Exception {
		writePomWithJavaSteps(
				"<licenseHeader>",
				"  <content>",
				"// If you can't trust a man's word",
				"// Does it help to have it in writing?",
				"  </content>",
				"</licenseHeader>");
		runTest();
	}

	@Test
	public void fromFileGlobal() throws Exception {
		setFile("license.txt").toResource(KEY_LICENSE);
		writePom("<licenseHeader>",
				"  <file>${basedir}/license.txt</file>",
				"</licenseHeader>",
				"<java/>");

		runTest();
	}

	private void runTest() throws Exception {
		setFile("src/main/java/test.java").toResource("license/MissingLicense.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		String actual = read("src/main/java/test.java");
		assertThat(actual).isEqualTo(getTestResource("license/HasLicense.test"));
	}
}
