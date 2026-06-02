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

class AsciidocHeadingHandlerTest {

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

	private static AsciidocFormatterFunc funcSetext() {
		return funcWith(cfg -> cfg.setNormalizeSetextHeadings(true));
	}

	private static AsciidocFormatterFunc funcTrailingEquals() {
		return funcWith(cfg -> cfg.setRemoveTrailingHeaderEqualsSign(true));
	}

	private static AsciidocFormatterFunc funcHeadingBlanks() {
		return funcWith(cfg -> cfg.setEnsureHeadingBlankLines(true));
	}

	private static String apply(AsciidocFormatterFunc f, String input) {
		try {
			return f.apply(input);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

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

	@Test
	void setextNormalizationThenHeadingBlankLinesThenTitleCase() {
		// Exercises the three ordering-dependent transformations in sequence:
		// setext → ATX (normalizeSetextHeadings), then blank-line padding
		// (ensureHeadingBlankLines), then title-casing (titleCase).
		AsciidocFormatterFunc f = funcWith(cfg -> {
			cfg.setNormalizeSetextHeadings(true);
			cfg.setEnsureHeadingBlankLines(true);
			cfg.setTitleCase(true);
		});
		String input = "some text\nmy cool section\n---------------\nsome body";
		assertThat(apply(f, input))
				.isEqualTo("some text\n\n== My Cool Section\n\nsome body");
	}

	@Test
	void crlfHeadingRecognizedAfterTrailingWhitespaceRemoval() {
		// Without removeTrailingWhitespace the \r ends up in the heading text;
		// verify that the combination produces correct output.
		AsciidocFormatterFunc f = funcWith(cfg -> {
			cfg.setRemoveTrailingWhitespace(true);
			cfg.setRemoveTrailingHeaderEqualsSign(true);
		});
		assertThat(apply(f, "== Title\r\n\r\nBody.\r\n"))
				.isEqualTo("== Title\n\nBody.\n");
	}
}
