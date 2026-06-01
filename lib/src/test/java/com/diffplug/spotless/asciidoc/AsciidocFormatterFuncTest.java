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

import org.junit.jupiter.api.Test;

class AsciidocFormatterFuncTest {

	// ── helpers ───────────────────────────────────────────────────────────────

	/** Config with only oneSentencePerLine toggled; everything else off. */
	private static AsciidocFormatterFunc func(boolean ospl) {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(ospl);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only normalizeSetextHeadings enabled. */
	private static AsciidocFormatterFunc funcSetext() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(true);
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
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only collapseConsecutiveBlankLines enabled. */
	private static AsciidocFormatterFunc funcCollapse() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(true);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only normalizeBlockDelimiters enabled. */
	private static AsciidocFormatterFunc funcDelimiters() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(true);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only removeTrailingHeaderEqualsSign enabled. */
	private static AsciidocFormatterFunc funcTrailingEquals() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(true);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only titleCase enabled. */
	private static AsciidocFormatterFunc funcTitleCase() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(true);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only removeTrailingWhitespace enabled. */
	private static AsciidocFormatterFunc funcTrailingWhitespace() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(true);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only normalizeListBullets enabled. */
	private static AsciidocFormatterFunc funcListBullets() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(true);
		cfg.setNormalizeOrderedListMarkers(false);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only normalizeOrderedListMarkers enabled. */
	private static AsciidocFormatterFunc funcOrderedList() {
		AsciidocFormatterConfig cfg = new AsciidocFormatterConfig();
		cfg.setNormalizeSetextHeadings(false);
		cfg.setCollapseConsecutiveBlankLines(false);
		cfg.setOneSentencePerLine(false);
		cfg.setNormalizeBlockDelimiters(false);
		cfg.setRemoveTrailingHeaderEqualsSign(false);
		cfg.setTitleCase(false);
		cfg.setRemoveTrailingWhitespace(false);
		cfg.setNormalizeListBullets(false);
		cfg.setNormalizeOrderedListMarkers(true);
		cfg.setEnsureHeadingBlankLines(false);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only ensureHeadingBlankLines enabled. */
	private static AsciidocFormatterFunc funcHeadingBlanks() {
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
		cfg.setEnsureHeadingBlankLines(true);
		cfg.setEnsureSourceDelimiters(false);
		return new AsciidocFormatterFunc(cfg);
	}

	/** Config with only ensureSourceDelimiters enabled. */
	private static AsciidocFormatterFunc funcSourceDelimiters() {
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
		cfg.setEnsureSourceDelimiters(true);
		return new AsciidocFormatterFunc(cfg);
	}

	private static String apply(AsciidocFormatterFunc f, String input) {
		try {
			return f.apply(input);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// ── normalizeSetextHeadings ───────────────────────────────────────────────

	@Test
	void convertsLevel0SetextHeading() {
		assertThat(apply(funcSetext(), "Document Title\n=============="))
				.isEqualTo("= Document Title");
	}

	@Test
	void convertsLevel1SetextHeading() {
		assertThat(apply(funcSetext(), "Section Title\n-------------"))
				.isEqualTo("== Section Title");
	}

	@Test
	void convertsLevel2SetextHeading() {
		assertThat(apply(funcSetext(), "Subsection Title\n~~~~~~~~~~~~~~~~"))
				.isEqualTo("=== Subsection Title");
	}

	@Test
	void convertsLevel3SetextHeading() {
		assertThat(apply(funcSetext(), "Deep Section\n^^^^^^^^^^^^"))
				.isEqualTo("==== Deep Section");
	}

	@Test
	void convertsLevel4SetextHeading() {
		assertThat(apply(funcSetext(), "Deepest Section\n+++++++++++++++"))
				.isEqualTo("===== Deepest Section");
	}

	@Test
	void convertsAllSetextLevelsInDocument() {
		String input = "Document\n========\n\nSection\n-------\n\nSubsection\n~~~~~~~~~~";
		assertThat(apply(funcSetext(), input)).isEqualTo(
				"= Document\n\n== Section\n\n=== Subsection");
	}

	@Test
	void doesNotConvertWhenUnderlineTooShort() {
		// underline shorter than title → not a setext heading
		assertThat(apply(funcSetext(), "Long Title Here\n---"))
				.isEqualTo("Long Title Here\n---");
	}

	@Test
	void doesNotConvertBlockDelimiterAsHeadingUnderline() {
		// ---- is a valid block delimiter length but also could be a setext underline;
		// the title "Hi" is shorter than "----" (4), so this is ambiguous —
		// the rule requires underline.length >= title.length, so "Hi\n----" IS converted.
		// A dedicated listing block "----\ncode\n----" must be left alone because there
		// is no title line before the first ----.
		assertThat(apply(funcSetext(), "----\ncode line\n----"))
				.isEqualTo("----\ncode line\n----");
	}

	@Test
	void doesNotConvertLineStartingWithEquals() {
		// lines starting with = are already atx headings, not setext title candidates
		assertThat(apply(funcSetext(), "== Already Atx\n==============="))
				.isEqualTo("== Already Atx\n===============");
	}

	@Test
	void doesNotConvertLineStartingWithBracket() {
		assertThat(apply(funcSetext(), "[source,java]\n============="))
				.isEqualTo("[source,java]\n=============");
	}

	@Test
	void doesNotConvertLineStartingWithSlash() {
		assertThat(apply(funcSetext(), "// comment\n==========="))
				.isEqualTo("// comment\n===========");
	}

	@Test
	void doesNotConvertClosingBracketBeforeBlockDelimiter() {
		// The lone ] line followed by ---- was falsely detected as a setext heading
		// (title + dash underline) because normalizeSetextHeadings lacked block tracking.
		String input = "[source, json]\n----\nusers: [\n  {\n    \"id\": \"abc\"\n  }\n]\n----";
		assertThat(apply(funcSetext(), input)).isEqualTo(input);
	}

	@Test
	void setextNormalizationIsIdempotent() throws Exception {
		String input = "My Title\n========\n\nA Section\n---------";
		String once = apply(funcSetext(), input);
		String twice = apply(funcSetext(), once);
		assertThat(twice).isEqualTo(once);
	}

	// ── collapseConsecutiveBlankLines ─────────────────────────────────────────

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

	// ── one sentence per line – basic ─────────────────────────────────────────

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

	// ── abbreviation handling ─────────────────────────────────────────────────

	@Test
	void doesNotSplitAfterDrAbbreviation() {
		String input = "Consult Dr. Smith before proceeding. Then continue.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Consult Dr. Smith before proceeding.\nThen continue.");
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

	// ── structural lines must pass through untouched ─────────────────────────

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

	// ── content inside delimited blocks must pass through untouched ──────────

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

	// ── block macros and page breaks must pass through untouched ────────────

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

	// ── blank lines separate paragraphs (no cross-paragraph joining) ─────────

	@Test
	void blankLineSeparatesParagraphs() {
		String input = "Paragraph one sentence one. Sentence two.\n\nParagraph two.";
		assertThat(apply(func(true), input)).isEqualTo(
				"Paragraph one sentence one.\nSentence two.\n\nParagraph two.");
	}

	// ── setext heading lookahead ──────────────────────────────────────────────

	@Test
	void doesNotMangleSetextHeading() {
		String input = "My Section\n----------\n\nParagraph text.";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	// ── normalizeBlockDelimiters ──────────────────────────────────────────────

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

	// ── removeTrailingHeaderEqualsSign ────────────────────────────────────────

	@Test
	void removesTrailingEqualsFromH2() {
		assertThat(apply(funcTrailingEquals(), "== Section Title =="))
				.isEqualTo("== Section Title");
	}

	@Test
	void removesTrailingEqualsFromH3() {
		assertThat(apply(funcTrailingEquals(), "=== Subsection ==="))
				.isEqualTo("=== Subsection");
	}

	@Test
	void removesTrailingEqualsFromH4() {
		assertThat(apply(funcTrailingEquals(), "==== Deep ==== Section ===="))
				.isEqualTo("==== Deep ==== Section");
	}

	@Test
	void removesTrailingEqualsWithTrailingSpaces() {
		assertThat(apply(funcTrailingEquals(), "== Title ==   "))
				.isEqualTo("== Title");
	}

	@Test
	void leavesAsymmetricHeadingUnchanged() {
		String input = "== Already Asymmetric";
		assertThat(apply(funcTrailingEquals(), input)).isEqualTo(input);
	}

	@Test
	void leavesNonHeadingLinesUnchanged() {
		String input = "Normal paragraph with == signs == inside.";
		assertThat(apply(funcTrailingEquals(), input)).isEqualTo(input);
	}

	@Test
	void removeTrailingEqualsIsIdempotent() throws Exception {
		String input = "== Title ==\n=== Sub ===";
		String once = apply(funcTrailingEquals(), input);
		String twice = apply(funcTrailingEquals(), once);
		assertThat(twice).isEqualTo(once);
	}

	// ── splitIntoSentences unit tests ─────────────────────────────────────────

	@Test
	void singleSentenceReturnedAsIs() {
		AsciidocFormatterFunc f = func(false);
		assertThat(f.splitIntoSentences("Just one sentence."))
				.containsExactly("Just one sentence.");
	}

	@Test
	void lowercaseAfterPeriodIsNotASplit() {
		AsciidocFormatterFunc f = func(false);
		assertThat(f.splitIntoSentences("lowercase follows. not a new sentence. no split here."))
				.containsExactly("lowercase follows. not a new sentence. no split here.");
	}

	// ── titleCase ─────────────────────────────────────────────────────────────

	@Test
	void titleCasesLevel1SectionHeading() {
		assertThat(apply(funcTitleCase(), "= examples of title case"))
				.isEqualTo("= Examples of Title Case");
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

	// ── removeTrailingWhitespace ──────────────────────────────────────────────

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

	// ── normalizeListBullets ──────────────────────────────────────────────────

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

	// ── normalizeOrderedListMarkers ───────────────────────────────────────────

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
	void numberedListWithTabNotMangledByOneSentencePerLine() {
		// "1.\titem" must be recognised as a list item (special line) so that
		// oneSentencePerLine does not join consecutive items into one long line
		String input = "1.\tFirst item\n2.\tSecond item\n3.\tThird item";
		assertThat(apply(func(true), input)).isEqualTo(input);
	}

	// ── removeTrailingHeaderEqualsSign – heading whitespace normalization ──────

	@Test
	void tabAfterHeadingMarkerNormalizedToSpace() {
		assertThat(apply(funcTrailingEquals(), "===\tNginx"))
				.isEqualTo("=== Nginx");
	}

	@Test
	void multipleSpacesAfterHeadingMarkerCollapsed() {
		assertThat(apply(funcTrailingEquals(), "==  Title"))
				.isEqualTo("== Title");
	}

	// ── ensureHeadingBlankLines ───────────────────────────────────────────────

	@Test
	void blankLineAddedAfterHeading() {
		assertThat(apply(funcHeadingBlanks(), "== Section\nContent"))
				.isEqualTo("== Section\n\nContent");
	}

	@Test
	void blankLineAddedBeforeHeading() {
		assertThat(apply(funcHeadingBlanks(), "Content\n== Section"))
				.isEqualTo("Content\n\n== Section");
	}

	@Test
	void blankLinesAddedBothSides() {
		assertThat(apply(funcHeadingBlanks(), "Before\n== Section\nAfter"))
				.isEqualTo("Before\n\n== Section\n\nAfter");
	}

	@Test
	void noDoubleBlankLineWhenAlreadyPresent() {
		String input = "Before\n\n== Section\n\nAfter";
		assertThat(apply(funcHeadingBlanks(), input)).isEqualTo(input);
	}

	@Test
	void noBlankLineBeforeFirstHeading() {
		assertThat(apply(funcHeadingBlanks(), "= Title\nContent"))
				.isEqualTo("= Title\n\nContent");
	}

	@Test
	void consecutiveHeadingsGetBlankLineBetweenThem() {
		assertThat(apply(funcHeadingBlanks(), "== Section A\n=== Subsection"))
				.isEqualTo("== Section A\n\n=== Subsection");
	}

	@Test
	void headingInsideCodeBlockGetsNoBlankLine() {
		// The "== heading" inside ---- must not acquire surrounding blank lines
		String input = "----\n== not a real heading\ncontent\n----";
		assertThat(apply(funcHeadingBlanks(), input)).isEqualTo(input);
	}

	@Test
	void ensureHeadingBlankLinesIsIdempotent() throws Exception {
		String input = "Intro\n== Section\nBody text\n=== Sub\nMore";
		String once = apply(funcHeadingBlanks(), input);
		String twice = apply(funcHeadingBlanks(), once);
		assertThat(twice).isEqualTo(once);
	}

	// ── ensureSourceDelimiters ────────────────────────────────────────────────

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
}
