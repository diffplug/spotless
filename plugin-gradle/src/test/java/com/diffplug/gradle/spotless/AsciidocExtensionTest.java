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
package com.diffplug.gradle.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class AsciidocExtensionTest extends GradleIntegrationHarness {

	private static final String[] BUILD_SCRIPT_DEFAULT = {
			"plugins {",
			"    id 'com.diffplug.spotless'",
			"}",
			"spotless {",
			"    asciidoc {",
			"        target '**/*.adoc'",
			"        asciidoc()",
			"    }",
			"}"
	};

	@Test
	void defaultFormattingApplied() throws IOException {
		setFile("build.gradle").toLines(BUILD_SCRIPT_DEFAULT);
		setFile("docs/sample.adoc").toResource("asciidoc/asciidocBefore.adoc");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("docs/sample.adoc").sameAsResource("asciidoc/asciidocAfter.adoc");
	}

	@Test
	void spotlessCheckFailsOnUnformattedThenPassesAfterApply() throws IOException {
		setFile("build.gradle").toLines(BUILD_SCRIPT_DEFAULT);
		setFile("docs/sample.adoc").toResource("asciidoc/asciidocBefore.adoc");

		String output = gradleRunner().withArguments("spotlessCheck").buildAndFail().getOutput();
		assertThat(output).contains("docs/sample.adoc");

		gradleRunner().withArguments("spotlessApply").build();
		gradleRunner().withArguments("spotlessCheck").build();
	}

	@Test
	void missingTargetFailsBuild() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    asciidoc {",
				"        asciidoc()",
				"    }",
				"}");
		setFile("docs/sample.adoc").toContent("= Title\n\nSome text.");

		String output = gradleRunner().withArguments("spotlessApply").buildAndFail().getOutput();
		assertThat(output).containsIgnoringCase("target");
	}

	@Test
	void titleCaseOptionApplied() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    asciidoc {",
				"        target '**/*.adoc'",
				"        asciidoc()",
				"            .normalizeSetextHeadings(false)",
				"            .collapseConsecutiveBlankLines(false)",
				"            .oneSentencePerLine(false)",
				"            .normalizeBlockDelimiters(false)",
				"            .removeTrailingHeaderEqualsSign(false)",
				"            .removeTrailingWhitespace(false)",
				"            .ensureHeadingBlankLines(false)",
				"            .titleCase(true)",
				"}",
				"}");
		setFile("docs/sample.adoc").toContent("= my document title\n\n== a section heading");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("docs/sample.adoc").hasContent("= My Document Title\n\n== A Section Heading");
	}

	@Test
	void ensureSourceDelimitersOptionApplied() throws IOException {
		setFile("build.gradle").toLines(
				"plugins {",
				"    id 'com.diffplug.spotless'",
				"}",
				"spotless {",
				"    asciidoc {",
				"        target '**/*.adoc'",
				"        asciidoc()",
				"            .normalizeSetextHeadings(false)",
				"            .collapseConsecutiveBlankLines(false)",
				"            .oneSentencePerLine(false)",
				"            .normalizeBlockDelimiters(false)",
				"            .removeTrailingHeaderEqualsSign(false)",
				"            .removeTrailingWhitespace(false)",
				"            .ensureHeadingBlankLines(false)",
				"            .ensureSourceDelimiters(true)",
				"}",
				"}");
		setFile("docs/sample.adoc").toContent("[source,java]\npublic void foo() {}");

		gradleRunner().withArguments("spotlessApply").build();

		assertFile("docs/sample.adoc").hasContent("[source,java]\n----\npublic void foo() {}\n----");
	}

	@Test
	void alreadyFormattedFileIsUpToDate() throws IOException {
		setFile("build.gradle").toLines(BUILD_SCRIPT_DEFAULT);
		setFile("docs/sample.adoc").toResource("asciidoc/asciidocAfter.adoc");

		applyIsUpToDate(false);
		applyIsUpToDate(true);
	}
}
