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
package com.diffplug.spotless.asciidoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A formatter function for Asciidoc that applies various formatting rules
 * based on the provided configuration.
 */
public class AsciidocFormatterFunc implements FormatterFunc {

	private final AsciidocFormatterConfig config;

	public AsciidocFormatterFunc(AsciidocFormatterConfig config) {
		this.config = config;
	}

	@NonNull @Override
	public String apply(@NonNull String input) throws Exception {
		// Use \R to match any line break (LF, CRLF, CR) and avoid multiple replacements
		List<String> lines = new ArrayList<>(Arrays.asList(Pattern.compile("\\R").split(input, -1)));

		AsciidocBlockHandler blockHandler = new AsciidocBlockHandler(lines);
		AsciidocHeadingHandler headingHandler = new AsciidocHeadingHandler(lines);
		AsciidocLineHandler lineHandler = new AsciidocLineHandler(lines);
		AsciidocSentenceHandler sentenceHandler = new AsciidocSentenceHandler(lines);

		// Ordering constraints:
		//   removeTrailingWhitespace  before  collapseConsecutiveBlankLines
		//     - whitespace-only lines must be emptied before they can be collapsed.
		//   normalizeSetextHeadings   before  ensureHeadingBlankLines
		//     - setext headings are converted to ATX first so they receive blank-line padding.
		if (config.isRemoveTrailingWhitespace()) {
			lineHandler.removeTrailingWhitespace();
		}
		if (config.isNormalizeSetextHeadings()) {
			headingHandler.normalizeSetextHeadings();
		}
		if (config.isEnsureSourceDelimiters()) {
			blockHandler.ensureSourceDelimiters();
		}
		if (config.isNormalizeBlockDelimiters()) {
			blockHandler.normalizeBlockDelimiters();
		}
		if (config.isRemoveTrailingHeaderEqualsSign()) {
			headingHandler.removeTrailingHeaderEqualsSign();
		}

		// Combine simple line-by-line transforms into a single in-place pass
		if (config.isTitleCase() || config.isNormalizeListBullets() || config.isNormalizeOrderedListMarkers()) {
			lineHandler.applyLineTransformations(config);
		}

		if (config.isEnsureHeadingBlankLines()) {
			headingHandler.ensureHeadingBlankLines();
		}
		if (config.isOneSentencePerLine()) {
			sentenceHandler.applySentencePerLine();
		}
		if (config.isCollapseConsecutiveBlankLines()) {
			lineHandler.collapseBlankLines();
		}

		return String.join("\n", lines);
	}
}
