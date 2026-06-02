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

class AsciidocLineHandlerTest {

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

	private static AsciidocFormatterFunc funcTitleCase() {
		return funcWith(cfg -> cfg.setTitleCase(true));
	}

	private static AsciidocFormatterFunc funcTrailingWhitespace() {
		return funcWith(cfg -> cfg.setRemoveTrailingWhitespace(true));
	}

	private static AsciidocFormatterFunc funcListBullets() {
		return funcWith(cfg -> cfg.setNormalizeListBullets(true));
	}

	private static AsciidocFormatterFunc funcOrderedList() {
		return funcWith(cfg -> cfg.setNormalizeOrderedListMarkers(true));
	}

	private static String apply(AsciidocFormatterFunc f, String input) {
		try {
			return f.apply(input);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void titleCasesLevel1SectionHeading() {
		assertThat(apply(funcTitleCase(), "= examples of title case"))
				.isEqualTo("= Examples of Title Case");
	}

	@Test
	void titleCaseHandlesWordsWithPunctuation() {
		assertThat(apply(funcTitleCase(), "== word, and another"))
				.isEqualTo("== Word, and Another");
	}

	@Test
	void titleCasesLevel2SectionHeading() {
		assertThat(apply(funcTitleCase(), "== the quick brown fox"))
				.isEqualTo("== The Quick Brown Fox");
	}

	@Test
	void titleCasesDeepSectionHeading() {
		assertThat(apply(funcTitleCase(), "==== art of the deal"))
				.isEqualTo("==== Art of the Deal");
	}

	@Test
	void titleCasesBlockTitle() {
		assertThat(apply(funcTitleCase(), ".examples of title case"))
				.isEqualTo(".Examples of Title Case");
	}

	@Test
	void firstWordAlwaysCapitalizedEvenIfInLowercaseSet() {
		// "of" is in the lowercase set but as the first word it must be capitalized
		assertThat(apply(funcTitleCase(), "== of mice and men"))
				.isEqualTo("== Of Mice and Men");
	}

	@Test
	void lastWordAlwaysCapitalized() {
		// "the" at the end must be capitalized
		assertThat(apply(funcTitleCase(), "== end of the"))
				.isEqualTo("== End of The");
	}

	@Test
	void articlesLowercasedInMiddle() {
		assertThat(apply(funcTitleCase(), "== the cat and the hat"))
				.isEqualTo("== The Cat and the Hat");
	}

	@Test
	void prepositionLowercasedInMiddle() {
		assertThat(apply(funcTitleCase(), "== art of war"))
				.isEqualTo("== Art of War");
	}

	@Test
	void coordinatingConjunctionLowercased() {
		assertThat(apply(funcTitleCase(), "== black or white"))
				.isEqualTo("== Black or White");
	}

	@Test
	void wordWithAttributeReferenceSkipped() {
		// {attr} contains special chars — must be left as-is
		assertThat(apply(funcTitleCase(), "== {doctitle} overview"))
				.isEqualTo("== {doctitle} Overview");
	}

	@Test
	void wordWithCodeSpanSkipped() {
		assertThat(apply(funcTitleCase(), "== use `code` here"))
				.isEqualTo("== Use `code` Here");
	}

	@Test
	void wordWithMacroSkipped() {
		// link:target[] is an AsciiDoc macro — skip the whole token
		assertThat(apply(funcTitleCase(), "== see link:url[] for details"))
				.isEqualTo("== See link:url[] for Details");
	}

	@Test
	void regularParagraphLineUntouched() {
		String input = "this is just regular paragraph text.";
		assertThat(apply(funcTitleCase(), input)).isEqualTo(input);
	}

	@Test
	void alreadyTitleCasedHeadingUnchanged() {
		String input = "== Examples of Title Case";
		assertThat(apply(funcTitleCase(), input)).isEqualTo(input);
	}

	@Test
	void titleCaseIsIdempotent() throws Exception {
		String input = "== examples of title case\n\n.a block title with of and the\n\nParagraph.";
		String once = apply(funcTitleCase(), input);
		String twice = apply(funcTitleCase(), once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void dotDotLineNotTreatedAsBlockTitle() {
		// ..something is not a block title (starts with ..)
		String input = "..not a block title";
		assertThat(apply(funcTitleCase(), input)).isEqualTo(input);
	}

	@Test
	void dotSpaceLineNotTreatedAsBlockTitle() {
		// ". item" starts with dot-space — that's an ordered list item, not a block title
		String input = ". list item text";
		assertThat(apply(funcTitleCase(), input)).isEqualTo(input);
	}

	@Test
	void trailingSpacesRemovedFromLine() {
		assertThat(apply(funcTrailingWhitespace(), "line with trailing spaces   "))
				.isEqualTo("line with trailing spaces");
	}

	@Test
	void trailingTabRemovedFromLine() {
		assertThat(apply(funcTrailingWhitespace(), "line with tab\t"))
				.isEqualTo("line with tab");
	}

	@Test
	void lineWithoutTrailingWhitespaceUnchanged() {
		String input = "clean line";
		assertThat(apply(funcTrailingWhitespace(), input)).isEqualTo(input);
	}

	@Test
	void blankLineReducedToEmpty() {
		assertThat(apply(funcTrailingWhitespace(), "   ")).isEqualTo("");
	}

	@Test
	void trailingWhitespaceRemovedFromMultipleLines() {
		assertThat(apply(funcTrailingWhitespace(), "first  \nsecond\t\nthird   "))
				.isEqualTo("first\nsecond\nthird");
	}

	@Test
	void removeTrailingWhitespaceIsIdempotent() throws Exception {
		String input = "line one   \nline two\t";
		String once = apply(funcTrailingWhitespace(), input);
		String twice = apply(funcTrailingWhitespace(), once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void dashListItemConvertedToAsterisk() {
		assertThat(apply(funcListBullets(), "- first item"))
				.isEqualTo("* first item");
	}

	@Test
	void multipleDashItemsAllConverted() {
		assertThat(apply(funcListBullets(), "- one\n- two\n- three"))
				.isEqualTo("* one\n* two\n* three");
	}

	@Test
	void asteriskListItemUnchanged() {
		String input = "* existing asterisk item";
		assertThat(apply(funcListBullets(), input)).isEqualTo(input);
	}

	@Test
	void nestedAsteriskItemsUnchanged() {
		String input = "* level one\n** level two\n*** level three";
		assertThat(apply(funcListBullets(), input)).isEqualTo(input);
	}

	@Test
	void dashInsideCodeBlockUntouched() {
		String input = "----\n- not a list item\n----";
		assertThat(apply(funcListBullets(), input)).isEqualTo(input);
	}

	@Test
	void blockDelimiterDashesNotConvertedToAsterisk() {
		// "----" is a block delimiter, not a list item
		String input = "----\ncode\n----";
		assertThat(apply(funcListBullets(), input)).isEqualTo(input);
	}

	@Test
	void listBulletsNormalizationIsIdempotent() throws Exception {
		String input = "- one\n- two";
		String once = apply(funcListBullets(), input);
		String twice = apply(funcListBullets(), once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void numberedListItemConvertedToAsciiDocStyle() {
		assertThat(apply(funcOrderedList(), "1. First item"))
				.isEqualTo(". First item");
	}

	@Test
	void largeNumberConvertedToAsciiDocStyle() {
		assertThat(apply(funcOrderedList(), "42. Some item"))
				.isEqualTo(". Some item");
	}

	@Test
	void multipleNumberedItemsAllConverted() {
		assertThat(apply(funcOrderedList(), "1. First\n2. Second\n3. Third"))
				.isEqualTo(". First\n. Second\n. Third");
	}

	@Test
	void asciiDocDotStyleUnchanged() {
		String input = ". First\n. Second";
		assertThat(apply(funcOrderedList(), input)).isEqualTo(input);
	}

	@Test
	void numberedListInsideCodeBlockUntouched() {
		String input = "----\n1. not a list item\n----";
		assertThat(apply(funcOrderedList(), input)).isEqualTo(input);
	}

	@Test
	void decimalNumberNotConvertedToListMarker() {
		// "3.14" does not match the "digit(s) dot space" pattern
		String input = "Version 3.14 is released.";
		assertThat(apply(funcOrderedList(), input)).isEqualTo(input);
	}

	@Test
	void orderedListNormalizationIsIdempotent() throws Exception {
		String input = "1. First\n2. Second\n3. Third";
		String once = apply(funcOrderedList(), input);
		String twice = apply(funcOrderedList(), once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void numberedListWithTabAfterNumberConverted() {
		// "1.\titem" must be treated the same as "1. item"
		assertThat(apply(funcOrderedList(), "1.\tFirst item"))
				.isEqualTo(". First item");
	}

	@Test
	void crlfLineEndingsNormalizedToLf() {
		// removeTrailingWhitespace strips \r, and join("\n") produces LF-only output
		assertThat(apply(funcTrailingWhitespace(), "line one\r\nline two\r\n"))
				.isEqualTo("line one\nline two\n");
	}
}
