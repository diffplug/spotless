/*
 * Copyright 2016-2021 DiffPlug
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

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class IndentTest extends MavenIntegrationHarness {

	@Test
	void fromContentToTabs() throws Exception {
		writePomWithFormatSteps(
				"<indent>",
				"  <tabs>true</tabs>",
				"</indent>");
		runToTabTest();
	}

	@Test
	void fromContentToSpaces() throws Exception {
		writePomWithFormatSteps(
				"<indent>",
				"  <spaces>true</spaces>",
				"</indent>");
		runToSpacesTest();
	}

	private void runToTabTest() throws Exception {
		runTest("indent/IndentedWithSpace.test", "indent/IndentedWithTab.test");
	}

	private void runToSpacesTest() throws Exception {
		runTest("indent/IndentedWithTab.test", "indent/IndentedWithSpace.test");
	}

	private void runTest(String source, String target) throws Exception {
		String path = "src/main/java/test.java";
		setFile(path).toResource(source);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource(target);
	}
}
