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

import java.io.IOException;

import org.junit.jupiter.api.Test;

class AsciidocExtensionTest extends GradleIntegrationHarness {
	@Test
	void integration() throws IOException {
		setFile("build.gradle")
				.toContent(
						"""
								plugins {
								    id 'com.diffplug.spotless'
								}
								repositories { mavenCentral() }
								spotless {
								    asciidoc {
								        target 'test.adoc'
										adocfmt('0.1.2')
										  .normalizeSetextHeadings(true)
										  .collapseConsecutiveBlankLines(true)
										  .oneSentencePerLine(true)
										  .normalizeBlockDelimiters(true)
										  .removeTrailingHeaderEqualsSign(true)
										  .titleCase(true)
										  .removeTrailingWhitespace(true)
										  .normalizeListBullets(true)
										  .normalizeOrderedListMarkers(true)
										  .ensureHeadingBlankLines(true)
										  .ensureSourceDelimiters(true)
								    }
								}
								""");
		setFile("test.adoc").toResource("asciidoc/adocfmt/dirty.adoc");
		gradleRunner().withArguments("spotlessApply").build();
		assertFile("test.adoc").sameAsResource("asciidoc/adocfmt/clean_all_options.adoc");
	}
}
