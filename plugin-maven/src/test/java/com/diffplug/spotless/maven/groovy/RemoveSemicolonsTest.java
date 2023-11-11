/*
 * Copyright 2023 DiffPlug
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
package com.diffplug.spotless.maven.groovy;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class RemoveSemicolonsTest extends MavenIntegrationHarness {

	@Test
	void testRemoveSemicolonsString() throws Exception {
		writePomWithGroovySteps("<removeSemicolons/>");
		runTest("Hello World;", "Hello World");
	}

	@Test
	void testNotRemoveSemicolonsString() throws Exception {
		writePomWithGroovySteps("<removeSemicolons/>");
		runTest("Hello;World", "Hello;World");
	}

	@Test
	void testRemoveSemicolons() throws Exception {
		writePomWithGroovySteps("<removeSemicolons/>");

		String path = "src/main/groovy/test.groovy";
		setFile(path).toResource("groovy/removesemicolons/GroovyCodeWithSemicolons.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("groovy/removesemicolons/GroovyCodeWithSemicolonsFormatted.test");

	}

	private void runTest(String sourceContent, String targetContent) throws Exception {
		String path = "src/main/groovy/test.groovy";
		setFile(path).toContent(sourceContent);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(targetContent);
	}
}
