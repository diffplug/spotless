/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.maven.antlr4;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class Antlr4FormatterTest extends MavenIntegrationHarness {

	@Test
	void applyUsingCustomVersion() throws Exception {
		writePomWithAntlr4Steps(
				"<antlr4Formatter>",
				"  <version>1.2.1</version>",
				"</antlr4Formatter>");
		runTest();
	}

	@Test
	void applyUsingDefaultVersion() throws Exception {
		writePomWithAntlr4Steps(
				"<antlr4Formatter>",
				"</antlr4Formatter>");
		runTest();
	}

	@Test
	void applyUsingDefaultVersionSelfclosing() throws Exception {
		writePomWithAntlr4Steps(
				"<antlr4Formatter />");
		runTest();
	}

	private void runTest() throws Exception {
		var path = "src/main/antlr4/Hello.g4";
		setFile(path).toResource("antlr4/Hello.unformatted.g4");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("antlr4/Hello.formatted.g4");
	}
}
