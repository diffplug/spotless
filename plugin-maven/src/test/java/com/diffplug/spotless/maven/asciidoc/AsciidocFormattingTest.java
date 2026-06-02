/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.maven.asciidoc;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class AsciidocFormattingTest extends MavenIntegrationHarness {

	private static final String TEST_FILE_PATH = "src/docs/index.adoc";

	@Test
	void defaultFormattingApply() throws Exception {
		writePomWithAsciidocSteps("<asciidocFormatting />");
		setFile(TEST_FILE_PATH).toResource("asciidoc/asciidocMavenDefaultBefore.adoc");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_FILE_PATH).sameAsResource("asciidoc/asciidocMavenDefaultAfter.adoc");
	}

	@Test
	void allNonDefaultOptionsEnabledApply() throws Exception {
		writePomWithAsciidocSteps(
				"<asciidocFormatting>",
				"  <normalizeSetextHeadings>true</normalizeSetextHeadings>",
				"  <oneSentencePerLine>true</oneSentencePerLine>",
				"  <normalizeBlockDelimiters>true</normalizeBlockDelimiters>",
				"  <removeTrailingHeaderEqualsSign>true</removeTrailingHeaderEqualsSign>",
				"</asciidocFormatting>");
		setFile(TEST_FILE_PATH).toResource("asciidoc/asciidocBefore.adoc");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(TEST_FILE_PATH).sameAsResource("asciidoc/asciidocAfter.adoc");
	}
}
