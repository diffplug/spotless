/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.maven.markdown;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class FlexmarkMavenTest extends MavenIntegrationHarness {

	@Test
	public void testFlexmarkWithSimpleConfig() throws Exception {
		writePomWithMarkdownSteps("<flexmark><pegdownExtensions>ALL,TOC</pegdownExtensions><extensions>YamlFrontMatter,Emoji</extensions></flexmark>");

		setFile("markdown_test.md").toResource("markdown/flexmark/FlexmarkUnformatted.md");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("markdown_test.md").sameAsResource("markdown/flexmark/FlexmarkFormatted.md");
	}

	@Test
	public void testFlexmarkWithComplexConfig() throws Exception {
		writePomWithMarkdownSteps("<flexmark><emulationProfile>COMMONMARK</emulationProfile><pegdownExtensions>0x0000FFFF</pegdownExtensions><extensions>com.vladsch.flexmark.ext.yaml.front.matter.YamlFrontMatterExtension</extensions></flexmark>");

		setFile("markdown_test.md").toResource("markdown/flexmark/FlexmarkUnformatted.md");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile("markdown_test.md").sameAsResource("markdown/flexmark/FlexmarkFormatted.md");
	}
}
