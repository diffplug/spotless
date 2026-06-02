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

class AsciidocSentenceHandlerTest {

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

	private static AsciidocFormatterFunc func(boolean ospl) {
		return funcWith(cfg -> cfg.setOneSentencePerLine(ospl));
	}

	private static AsciidocFormatterFunc funcCollapse() {
		return funcWith(cfg -> cfg.setCollapseConsecutiveBlankLines(true));
	}

	private static String apply(AsciidocFormatterFunc f, String input) {
		try {
			return f.apply(input);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void singleBlankLinePreserved() {
		assertThat(apply(funcCollapse(), "A\n\nB")).isEqualTo("A\n\nB");
	}

	@Test
	void twoBlankLinesCollapsedToOne() {
		assertThat(apply(funcCollapse(), "A\n\n\nB")).isEqualTo("A\n\nB");
	}

	@Test
	void threeBlankLinesCollapsedToOne() {
		assertThat(apply(funcCollapse(), "A\n\n\n\nB")).isEqualTo("A\n\nB");
	}

	@Test
	void noBlankLinesUnchanged() {
		assertThat(apply(funcCollapse(), "A\nB\nC")).isEqualTo("A\nB\nC");
	}

	@Test
	void multipleGroupsEachCollapsed() {
		assertThat(apply(funcCollapse(), "A\n\n\nB\n\n\n\nC")).isEqualTo("A\n\nB\n\nC");
	}

	@Test
	void leadingBlankLinesCollapsed() {
		assertThat(apply(funcCollapse(), "\n\n\nA")).isEqualTo("\nA");
	}

	@Test
	void trailingBlankLinesCollapsed() {
		assertThat(apply(funcCollapse(), "A\n\n\n")).isEqualTo("A\n");
	}

	@Test
	void collapseIsIdempotent() throws Exception {
		String input = "A\n\n\nB\n\n\n\nC";
		String once = apply(funcCollapse(), input);
		String twice = apply(funcCollapse(), once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void splitsTwoSentencesOnOneLine() {
		String input = "First sentence. Second sentence.";
		assertThat(apply(func(true), input)).isEqualTo(
				"First sentence.\nSecond sentence.");
	}

	@Test
	void splitsExclamationAndQuestion() {
		String input = "Watch out! Are you sure? Proceed anyway.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Watch out!\nAre you sure?\nProceed anyway.");
	}

	@Test
	void exclamationSplitsBeforeLowercaseWord() {
		// Regression: '!' previously required the following word to start with
		// uppercase, so "Stop! don't move." was never split.
		assertThat(apply(func(true), "Stop! don't move. Please continue."))
				.isEqualTo("Stop!\ndon't move.\nPlease continue.");
	}

	@Test
	void questionMarkSplitsBeforeLowercaseWord() {
		// Regression: '?' previously required the following word to start with
		// uppercase, so "Really? maybe not." was never split.
		assertThat(apply(func(true), "Really? maybe not. Let's check."))
				.isEqualTo("Really?\nmaybe not.\nLet's check.");
	}

	@Test
	void joinsMultiLineParagraphThenSplits() {
		String input = "This is a long sentence that\nspans multiple lines. Second sentence.";
		assertThat(apply(func(true), input)).isEqualTo(
				"This is a long sentence that spans multiple lines.\nSecond sentence.");
	}

	@Test
	void idempotent() throws Exception {
		String input = "First sentence. Second sentence.\nThird sentence.";
		AsciidocFormatterFunc f = func(true);
		String once = apply(f, input);
		String twice = apply(f, once);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	void drAbbreviationIsNotASentenceBoundary() {
		String input = "Consult Dr. Smith before proceeding. Then continue.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Consult Dr. Smith before proceeding.\nThen continue.");
	}

	@Test
	void initialIsNotASentenceBoundary() {
		String input = "The author is A. Smith. He is famous.";
		assertThat(apply(func(true), input)).isEqualTo(
				"The author is A. Smith.\nHe is famous.");
	}

	@Test
	void abbreviationFollowedByCapitalIsNotASentenceBoundary() {
		String input = "Item etc. And more. Next sentence.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Item etc. And more.\nNext sentence.");
	}

	@Test
	void blockTitleIsSpecialLine() {
		String input = ".Block Title\nThis is a sentence. This is another.";
		assertThat(apply(func(true), input)).isEqualTo(
				".Block Title\nThis is a sentence.\nThis is another.");
	}

	@Test
	void doesNotSplitInsideEgAbbreviation() {
		String input = "Use a tool (e.g. Spotless) for formatting. It helps.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Use a tool (e.g. Spotless) for formatting.\nIt helps.");
	}

	@Test
	void doesNotSplitDecimalNumber() {
		String input = "The value is 3.14 approximately. Use it wisely.";
		assertThat(apply(func(true), input)).isEqualTo(
				"The value is 3.14 approximately.\nUse it wisely.");
	}

	@Test
	void doesNotSplitEllipsis() {
		String input = "Well... that is interesting. Next point.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Well... that is interesting.\nNext point.");
	}

	@Test
	void doesNotTouchHeadings() {
		String input = "== Section Title\n\nParagraph text.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void doesNotTouchAttributeEntries() {
		String input = ":my-attr: some value\n\nParagraph.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void doesNotTouchBlockAttributes() {
		String input = "[source,java]\n----\ncode here\n----\n\nText.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void doesNotTouchListItems() {
		String input = "* First item. Still item.\n* Second item.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void doesNotReformatInsideListingBlock() {
		String input = "----\nFirst sentence. Second sentence.\n----";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void doesNotReformatInsideExampleBlock() {
		String input = "====\nFirst sentence. Second sentence.\n====";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void pageBreakIsNotJoinedWithAdjacentMacros() {
		// toc::[], <<<, and include:: are structural – they must never be accumulated
		// into a paragraph and joined into a single line
		String input = "toc::[]\n<<<\ninclude::file.adoc[]";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void pageBreakBetweenParagraphsPassedThrough() {
		String input = "First paragraph.\n<<<\nSecond paragraph.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void includeDirectiveNotJoinedWithParagraph() {
		String input = "include::chapter.adoc[]\n\nSome text.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void tocMacroPassedThrough() {
		assertThat(apply(func(true), "toc::[]")).isEqualTo("toc::[]");
	}

	@Test
	void horizontalRulePassedThrough() {
		assertThat(apply(func(true), "Sentence one.\n'''\nSentence two."))
				.isEqualTo("Sentence one.\n'''\nSentence two.");
	}

	@Test
	void dashHorizontalRulePassedThrough() {
		assertThat(apply(func(true), "Sentence one.\n---\nSentence two."))
				.isEqualTo("Sentence one.\n---\nSentence two.");
	}

	@Test
	void asteriskHorizontalRulePassedThrough() {
		assertThat(apply(func(true), "Sentence one.\n***\nSentence two."))
				.isEqualTo("Sentence one.\n***\nSentence two.");
	}

	@Test
	void blankLineSeparatesParagraphs() {
		String input = "Paragraph one sentence one. Sentence two.\n\nParagraph two.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Paragraph one sentence one.\nSentence two.\n\nParagraph two.");
	}

	@Test
	void doesNotMangleSetextHeading() {
		String input = "My Section\n----------\n\nParagraph text.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	@Test
	void singleSentenceReturnedAsIs() {
		assertThat(apply(func(true), "Just one sentence.")).isEqualTo("Just one sentence.");
	}

	@Test
	void lowercaseAfterPeriodIsNotASplit() {
		assertThat(apply(func(true), "lowercase follows. not a new sentence. no split here."))
				.isEqualTo("lowercase follows. not a new sentence. no split here.");
	}

	@Test
	void numberedListWithTabNotMangledByOneSentencePerLine() {
		// "1.\titem" must be recognised as a list item (special line) so that
		// oneSentencePerLine does not join consecutive items into one long line
		String input = "1.\tFirst item\n2.\tSecond item\n3.\tThird item";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}
}
