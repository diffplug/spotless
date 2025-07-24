/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.maven.java;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class ImportOrderTest extends MavenIntegrationHarness {
	@Test
	void file() throws Exception {
		setFile("import.properties").toResource("java/importsorter/import.properties");
		writePomWithJavaSteps(
				"<importOrder>",
				"  <file>${basedir}/import.properties</file>",
				"</importOrder>");
		runTest();
	}

	@Test
	void order() throws Exception {
		writePomWithJavaSteps(
				"<importOrder>",
				"  <order>java,javax,org,\\#com</order>",
				"</importOrder>");
		runTest();
	}

	@Test
	void standard() throws Exception {
		writePomWithJavaSteps("<importOrder />");
		runTest("java/importsorter/JavaCodeSortedImportsDefault.test");
	}

	@Test
	void wildcardsLast() throws Exception {
		writePomWithJavaSteps(
				"<importOrder>",
				"  <wildcardsLast>true</wildcardsLast>",
				"</importOrder>");
		runTest("java/importsorter/JavaCodeSortedImportsWildcardsLast.test");
	}

	private void runTest() throws Exception {
		runTest("java/importsorter/JavaCodeSortedImports.test");
	}

	private void runTest(String expectedResource) throws Exception {
		setFile(TEST_PATH).toResource("java/importsorter/JavaCodeUnsortedImports.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_PATH).sameAsResource(expectedResource);
	}
}
