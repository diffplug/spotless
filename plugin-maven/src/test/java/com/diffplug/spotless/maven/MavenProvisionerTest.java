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
package com.diffplug.spotless.maven;

import org.junit.jupiter.api.Test;

class MavenProvisionerTest extends MavenIntegrationHarness {

	@Test
	void testMultipleDependenciesExcludingTransitives() throws Exception {
		writePomWithJavaSteps(
				"<eclipse>",
				"  <version>4.8.0</version>",
				"</eclipse>");
		setFile("formatter.xml").toResource("java/eclipse/formatter.xml");
		assertResolveDependenciesWorks();
	}

	@Test
	void testSingleDependencyIncludingTransitives() throws Exception {
		writePomWithJavaSteps(
				"<googleJavaFormat>",
				"  <version>1.2</version>",
				"</googleJavaFormat>");
		assertResolveDependenciesWorks();
	}

	private void assertResolveDependenciesWorks() throws Exception {
		String path = "src/main/java/test.java";
		String unformattedContent = "package  a;";
		setFile(path).toContent(unformattedContent);
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).hasContent(unformattedContent.replace("  ", " "));
	}
}
