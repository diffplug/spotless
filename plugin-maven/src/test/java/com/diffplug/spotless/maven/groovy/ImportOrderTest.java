/*
 * Copyright 2020-2021 DiffPlug
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

class ImportOrderTest extends MavenIntegrationHarness {
	@Test
	void file() throws Exception {
		setFile("import.properties").toResource("java/importsorter/import.properties");
		writePomWithGroovySteps(
				"<importOrder>",
				"  <file>${basedir}/import.properties</file>",
				"</importOrder>");
		runTest();
	}

	@Test
	void order() throws Exception {
		writePomWithGroovySteps(
				"<importOrder>",
				"  <order>java,javax,org,\\#com</order>",
				"</importOrder>");
		runTest();
	}

	@Test
	void standard() throws Exception {
		writePomWithGroovySteps("<importOrder />");
		runTest("java/importsorter/GroovyCodeSortedMisplacedImportsDefault.test");
	}

	private void runTest() throws Exception {
		runTest("java/importsorter/GroovyCodeSortedMisplacedImports.test");
	}

	private void runTest(String expectedResource) throws Exception {
		String path = "src/main/groovy/test.groovy";
		setFile(path).toResource("java/importsorter/GroovyCodeUnsortedMisplacedImports.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource(expectedResource);
	}
}
