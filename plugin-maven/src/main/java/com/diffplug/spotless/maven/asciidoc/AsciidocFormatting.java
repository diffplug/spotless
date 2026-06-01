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

import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.asciidoc.AsciidocFormatterConfig;
import com.diffplug.spotless.asciidoc.AsciidocFormatterStep;
import com.diffplug.spotless.maven.FormatterStepConfig;
import com.diffplug.spotless.maven.FormatterStepFactory;

public class AsciidocFormatting implements FormatterStepFactory {

	@Parameter
	private boolean normalizeSetextHeadings = false;

	@Parameter
	private boolean collapseConsecutiveBlankLines = true;

	@Parameter
	private boolean oneSentencePerLine = false;

	@Parameter
	private boolean normalizeBlockDelimiters = false;

	@Parameter
	private boolean removeTrailingHeaderEqualsSign = false;

	@Parameter
	private boolean titleCase = false;

	@Parameter
	private boolean removeTrailingWhitespace = true;

	@Parameter
	private boolean normalizeListBullets = false;

	@Parameter
	private boolean normalizeOrderedListMarkers = false;

	@Parameter
	private boolean ensureHeadingBlankLines = true;

	@Parameter
	private boolean ensureSourceDelimiters = false;

	@Override
	public FormatterStep newFormatterStep(FormatterStepConfig config) {
		AsciidocFormatterConfig asciidocConfig = new AsciidocFormatterConfig();
		asciidocConfig.setNormalizeSetextHeadings(normalizeSetextHeadings);
		asciidocConfig.setCollapseConsecutiveBlankLines(collapseConsecutiveBlankLines);
		asciidocConfig.setOneSentencePerLine(oneSentencePerLine);
		asciidocConfig.setNormalizeBlockDelimiters(normalizeBlockDelimiters);
		asciidocConfig.setRemoveTrailingHeaderEqualsSign(removeTrailingHeaderEqualsSign);
		asciidocConfig.setTitleCase(titleCase);
		asciidocConfig.setRemoveTrailingWhitespace(removeTrailingWhitespace);
		asciidocConfig.setNormalizeListBullets(normalizeListBullets);
		asciidocConfig.setNormalizeOrderedListMarkers(normalizeOrderedListMarkers);
		asciidocConfig.setEnsureHeadingBlankLines(ensureHeadingBlankLines);
		asciidocConfig.setEnsureSourceDelimiters(ensureSourceDelimiters);
		return AsciidocFormatterStep.create(asciidocConfig);
	}
}
