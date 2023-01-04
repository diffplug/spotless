/*
 * Copyright 2021 DiffPlug
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

public class FlexmarkMavenTest extends MavenIntegrationHarness {

	@Test
	public void testFlexmarkWithDefaultConfig() throws Exception {
		writePomWithMarkdownSteps("<flexmark />");

		setFile("markdown_test.md").toResource("markdown/flexmark/FlexmarkUnformatted.md");
		mavenRunner().withArguments("spotless:apply").runNoError().error();
		assertFile("markdown_test.md").sameAsResource("markdown/flexmark/FlexmarkFormatted.md");
	}


	@Test
	public void testFlexmarkWithDefaultConfig_endsWithCodeBlock_withNewLine() throws Exception {
		List<String> configurations = new ArrayList<>();

		configurations.addAll(Arrays.asList(formats(groupWithSteps("format", including("*.md"), "<endWithNewline/>"))));
		configurations.addAll(Arrays.asList(groupWithSteps("markdown", including("**/*.md"), "<flexmark />")));

		writePom(configurations.toArray(new String[0]));

		setFile("markdown_test.md").toResource("markdown/flexmark/EndsWithCodeBlockUnformatted.md");
		mavenRunner().withArguments("spotless:apply").runNoError().error();
		assertFile("markdown_test.md").sameAsResource("markdown/flexmark/EndsWithCodeBlockFormattedWithNewLine.md");

		mavenRunner().withArguments("spotless:check").runNoError().error();
	}

}
