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

class AsciidocBlockHandlerTest {

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

	private static AsciidocFormatterFunc funcDelimiters() {
		return funcWith(cfg -> cfg.setNormalizeBlockDelimiters(true));
	}

	private static AsciidocFormatterFunc funcSourceDelimiters() {
		return funcWith(cfg -> cfg.setEnsureSourceDelimiters(true));
	}

	private static String apply(AsciidocFormatterFunc f, String input) {
		try {
			return f.apply(input);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void shortensLongDashDelimiter() {
		assertThat(apply(funcDelimiters(), "--------\ncode\n--------"))
				.isEqualTo("----\ncode\n----");
	}

	@Test
	void shortensLongEqualsDelimiter() {
		assertThat(apply(funcDelimiters(), "========\ncontent\n========"))
				.isEqualTo("====\ncontent\n====");
	}

	@Test
	void shortensLongDotDelimiter() {
		assertThat(apply(funcDelimiters(), "........\nliteral\n........"))
				.isEqualTo("....\nliteral\n....");
	}

	@Test
	void shortensLongStarDelimiter() {
		assertThat(apply(funcDelimiters(), "********\nsidebar\n********"))
				.isEqualTo("****\nsidebar\n****");
	}

	@Test
	void shortensLongUnderscoreDelimiter() {
		assertThat(apply(funcDelimiters(), "________\nquote\n________"))
				.isEqualTo("____\nquote\n____");
	}

	@Test
	void shortensLongPlusDelimiter() {
		assertThat(apply(funcDelimiters(), "++++++++\npass\n++++++++"))
				.isEqualTo("++++\npass\n++++");
	}

	@Test
	void shortensLongSlashDelimiter() {
		assertThat(apply(funcDelimiters(), "////////\ncomment\n////////"))
				.isEqualTo("////\ncomment\n////");
	}

	@Test
	void leavesMinimalDelimiterUnchanged() {
		String input = "----\ncode\n----";
		assertThat(apply(funcDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void doesNotShortenSetextHeadingUnderline() {
		// ==============  is a setext heading underline (preceded by a title),
		// not a block delimiter, so it must not be shortened.
		String input = "Document Title\n==============";
		assertThat(apply(funcDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void doesNotShortenTildeSetextUnderline() {
		// ~ is not a block-delimiter character, so ~~~~~~~ is always a setext underline.
		String input = "Subsection\n~~~~~~~~~~";
		assertThat(apply(funcDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void blockDelimiterNormalizationIsIdempotent() throws Exception {
		String input = "--------\ncode\n--------\n\n========\nblock\n========";
		String once = apply(funcDelimiters(), input);
		String twice = apply(funcDelimiters(), once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void sourceBlockWithoutDelimiterGetsWrapped() {
		String input = "[source,java]\npublic void foo() {}";
		assertThat(apply(funcSourceDelimiters(), input))
				.isEqualTo("[source,java]\n----\npublic void foo() {}\n----");
	}

	@Test
	void sourceBlockAlreadyDelimitedLeftUnchanged() {
		String input = "[source,java]\n----\npublic void foo() {}\n----";
		assertThat(apply(funcSourceDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void listingBlockWithoutDelimiterGetsWrapped() {
		String input = "[listing]\nsome literal text";
		assertThat(apply(funcSourceDelimiters(), input))
				.isEqualTo("[listing]\n----\nsome literal text\n----");
	}

	@Test
	void multiLineSourceBlockWrapped() {
		String input = "[source,yaml]\nkey: value\nother: data";
		assertThat(apply(funcSourceDelimiters(), input))
				.isEqualTo("[source,yaml]\n----\nkey: value\nother: data\n----");
	}

	@Test
	void sourceBlockFollowedByBlankLineNotWrapped() {
		// blank line after the attribute means no content to wrap
		String input = "[source,java]\n\nsome text";
		assertThat(apply(funcSourceDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void sourceBlockFollowedByAnotherAttributeNotWrapped() {
		// next line is another block attribute; leave it alone
		String input = "[source,java]\n[%linenums]\n----\ncode\n----";
		assertThat(apply(funcSourceDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void sourceBlockWithLanguageVariantsWrapped() {
		String input = "[source, json]\n{\"key\": \"value\"}";
		assertThat(apply(funcSourceDelimiters(), input))
				.isEqualTo("[source, json]\n----\n{\"key\": \"value\"}\n----");
	}

	@Test
	void sourceWithPercentOptionWrapped() {
		String input = "[source%autofit,java]\npublic class Foo {}";
		assertThat(apply(funcSourceDelimiters(), input))
				.isEqualTo("[source%autofit,java]\n----\npublic class Foo {}\n----");
	}

	@Test
	void sourceBlockInsideExistingDelimitedBlockLeftAlone() {
		// [source] inside ==== must not be touched because we're inside a block
		String input = "====\n[source,java]\ncode\n====";
		assertThat(apply(funcSourceDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void ensureSourceDelimitersIsIdempotent() throws Exception {
		String input = "[source,java]\npublic void foo() {}\n\n[source,yaml]\nkey: value";
		String once = apply(funcSourceDelimiters(), input);
		String twice = apply(funcSourceDelimiters(), once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void overLongDelimiterRecognizedAsExistingDelimiter() {
		// "--------" (over-long) counts as an existing delimiter — we don't add another ----
		String input = "[source,java]\n--------\ncode\n--------";
		assertThat(apply(funcSourceDelimiters(), input)).isEqualTo(input);
	}

	@Test
	void sourceBlockWithIdShorthandGetsWrapped() {
		// [source#id,lang] uses AsciiDoc shorthand — must be recognized and wrapped
		String input = "[source#intro,java]\npublic void foo() {}";
		assertThat(apply(funcSourceDelimiters(), input))
				.isEqualTo("[source#intro,java]\n----\npublic void foo() {}\n----");
	}
}
