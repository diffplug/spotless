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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class AsciidocFormatterFuncTest {

	private static AsciidocFormatterFunc funcWith(Consumer<AsciidocFormatterConfig> customizer) {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		customizer.accept(cfg);
		return new AsciidocFormatterFunc(cfg);
	}

	@Test
	void removeTrailingEqualsRunsBeforeSetextNormalization() throws Exception {
		// Ordering constraint: removeTrailingHeaderEqualsSign must run before
		// normalizeSetextHeadings. If the order were reversed, "Config =\n========"
		// would first become "= Config =" and then have the trailing '=' stripped as
		// symmetric decoration, yielding "= Config" instead of "= Config =".
		AsciidocFormatterFunc f = funcWith(cfg -> {
			cfg.setRemoveTrailingHeaderEqualsSign(true);
			cfg.setNormalizeSetextHeadings(true);
		});
		assertThat(f.apply("Config =\n========")).isEqualTo("= Config =");
	}

	@Test
	void setextNormalizationRunsBeforeHeadingBlankLines() throws Exception {
		// Ordering constraint: normalizeSetextHeadings must run before
		// ensureHeadingBlankLines. If the order were reversed, ensureHeadingBlankLines
		// would see a plain paragraph line (the setext title candidate) and add no
		// padding; the converted ATX heading would then lack its surrounding blank lines.
		AsciidocFormatterFunc f = funcWith(cfg -> {
			cfg.setNormalizeSetextHeadings(true);
			cfg.setEnsureHeadingBlankLines(true);
		});
		assertThat(f.apply("Before\nSection Title\n=============\nAfter"))
				.isEqualTo("Before\n\n= Section Title\n\nAfter");
	}

	@Test
	void appliesMultipleFormattingRules() throws Exception {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(true);
		cfg.setNormalizeBlockDelimiters(true);
		cfg.setTitleCase(true);
		cfg.setOneSentencePerLine(true);
		cfg.setNormalizeListBullets(true);
		cfg.setEnsureSourceDelimiters(true);

		AsciidocFormatterFunc func = new AsciidocFormatterFunc(cfg);

		String input = "my title\n========\n\n- list item one. list item two.\n\n[source, java]\npublic void foo() {}";
		String expected = "= My Title\n\n* list item one. list item two.\n\n[source, java]\n----\npublic void foo() {}\n----";

		assertThat(func.apply(input)).isEqualTo(expected);
	}

	@Test
	void appliesNoFormattingWhenConfigDisabled() throws Exception {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		// All defaults are false

		AsciidocFormatterFunc func = new AsciidocFormatterFunc(cfg);

		String input = "some text";

		assertThat(func.apply(input)).isEqualTo(input);
	}
}
