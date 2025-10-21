/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.glue.gherkin;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.gherkin.GherkinUtilsConfig;

import io.cucumber.gherkin.GherkinParser;
import io.cucumber.gherkin.utils.pretty.Pretty;
import io.cucumber.gherkin.utils.pretty.Syntax;
import io.cucumber.messages.types.Envelope;
import io.cucumber.messages.types.GherkinDocument;
import io.cucumber.messages.types.Source;
import io.cucumber.messages.types.SourceMediaType;

public class GherkinUtilsFormatterFunc implements FormatterFunc {

	public GherkinUtilsFormatterFunc(GherkinUtilsConfig gherkinSimpleConfig) {

	}

	// Follows https://github.com/cucumber/gherkin-utils/blob/main/java/src/test/java/io/cucumber/gherkin/utils/pretty/PrettyTest.java
	private GherkinDocument parse(String gherkin) {
		GherkinParser parser = GherkinParser
				.builder()
				.includeSource(false)
				.build();
		return parser.parse(Envelope.of(new Source("test.feature", gherkin, SourceMediaType.TEXT_X_CUCUMBER_GHERKIN_PLAIN)))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("No envelope"))
				.getGherkinDocument()
				.orElseThrow(() -> new IllegalArgumentException("No gherkin document"));
	}

	@Override
	public String apply(String inputString) {
		GherkinDocument gherkinDocument = parse(inputString);

		return Pretty.prettyPrint(gherkinDocument, Syntax.gherkin);
	}
}
