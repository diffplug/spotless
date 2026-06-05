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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.ProcessRunner;
import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class AdocfmtMavenTest extends MavenIntegrationHarness {
	@Test
	public void missingIncludesFails() throws Exception {
		writePom(groupWithSteps("asciidoc",
				"<adocfmt>",
				"  <version>0.2.0</version>",
				"</adocfmt>"));
		ProcessRunner.Result result = mavenRunner().withArguments("spotless:apply").runHasError();
		assertThat(result.stdOutUtf8()).contains("You must specify some files to include");
	}

	@Test
	public void testAdocfmt() throws Exception {
		writePomWithAsciidocSteps(
				"""
						<adocfmt>
						  <version>0.2.0</version> <!-- optional -->
						  <normalizeSetextHeadings>true</normalizeSetextHeadings>
						  <collapseConsecutiveBlankLines>true</collapseConsecutiveBlankLines>
						  <oneSentencePerLine>true</oneSentencePerLine>
						  <normalizeBlockDelimiters>true</normalizeBlockDelimiters>
						  <removeTrailingHeaderEqualsSign>true</removeTrailingHeaderEqualsSign>
						  <titleCase>true</titleCase>
						  <removeTrailingWhitespace>true</removeTrailingWhitespace>
						  <normalizeListBullets>true</normalizeListBullets>
						  <normalizeOrderedListMarkers>true</normalizeOrderedListMarkers>
						  <ensureHeadingBlankLines>true</ensureHeadingBlankLines>
						  <ensureSourceDelimiters>true</ensureSourceDelimiters>
						</adocfmt>
						""");

		setFile("test.adoc").toResource("asciidoc/adocfmt/dirty.adoc");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("test.adoc").sameAsResource("asciidoc/adocfmt/clean_all_options.adoc");
	}
}
