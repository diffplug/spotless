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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;

public class AsciidocFormatterFunc implements FormatterFunc {

	// ── constants ─────────────────────────────────────────────────────────────

	// Single source of truth for block-delimiter characters; isBlockDelimiter is derived from this.
	// To add a new delimiter type, append its character here only.
	private static final String BLOCK_DELIMITER_CHARS = "-=.*_+/";

	// Heading with trailing = signs: == Title == or === Title ===
	// Captured groups: (1) leading equals, (2) title text (trimmed)
	private static final Pattern SYMMETRIC_HEADING = Pattern.compile("^(={1,6})\\s+(.*\\S)\\s+=+\\s*$");

	// Section heading: = Title or == Title, etc.
	// Captured groups: (1) leading equals, (2) title text
	private static final Pattern SECTION_HEADING = Pattern.compile("^(={1,6})\\s+(.+)$");

	// Words lowercased in title case (articles, conjunctions, short prepositions)
	private static final Set<String> TITLE_CASE_LOWERCASE = Set.of(
			"a", "an", "the",
			"and", "but", "or", "nor", "for", "yet", "so",
			"at", "by", "in", "of", "on", "to", "up", "as", "off", "out", "per", "via");

	// Any ATX heading, used to normalise whitespace (tab → space) after the = signs
	// Captured groups: (1) leading equals, (2) trimmed title text
	private static final Pattern ATX_HEADING = Pattern.compile("^(={1,6})\\s+(\\S.*?)\\s*$");

	// Source / listing block attribute lines: [source], [source,java], [listing], [source%linenums,java], [source#id,java], etc.
	private static final Pattern SOURCE_BLOCK_ATTR = Pattern.compile("^\\[(source|listing)[,\\]%#].*");

	// ATX heading prefixes for setext→ATX conversion: ATX_PREFIX[n] = "=".repeat(n+1) + " "
	private static final String[] ATX_PREFIX = {"= ", "== ", "=== ", "==== ", "===== ", "====== "};

	// Pre-compiled whitespace-run pattern used to collapse internal whitespace in flushParagraph.
	private static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");

	// Known abbreviations that end with a period but do not end a sentence
	private static final Set<String> ABBREVIATIONS = Set.of(
			"mr", "mrs", "ms", "dr", "prof", "sr", "jr",
			"vs", "etc", "approx", "dept", "fig", "no", "vol",
			"ch", "sec", "ref", "rev", "st", "mt", "ft",
			"ave", "blvd", "rd", "pp", "al", "ed", "eds",
			"corp", "inc", "ltd", "llc",
			"jan", "feb", "mar", "apr", "jun", "jul",
			"aug", "sep", "sept", "oct", "nov", "dec",
			// German abbreviations
			"bspw", "bzw", "bzgl", "ca", "evtl", "exkl", "inkl", "sog");

	private final AsciidocFormatterConfig config;

	public AsciidocFormatterFunc(AsciidocFormatterConfig config) {
		this.config = config;
	}

	// ── entry point ───────────────────────────────────────────────────────────

	@Override
	public String apply(String input) throws Exception {
		// Normalize line endings so every downstream method sees only \n.
		// Without this, CRLF input leaves \r on each line, which breaks regex matches
		// (e.g. detectSetextUnderline's length check) and embeds \r in heading output.
		input = input.replace("\r\n", "\n").replace("\r", "\n");
		String[] lines = input.split("\n", -1);

		// Ordering constraints:
		//   removeTrailingWhitespace  before  collapseConsecutiveBlankLines
		//     — whitespace-only lines must be emptied before they can be collapsed.
		//   normalizeSetextHeadings   before  ensureHeadingBlankLines
		//     — setext headings are converted to ATX first so they receive blank-line padding.
		if (config.isRemoveTrailingWhitespace()) {
			lines = removeTrailingWhitespace(lines);
		}
		if (config.isEnsureSourceDelimiters()) {
			lines = ensureSourceDelimiters(lines);
		}
		if (config.isNormalizeSetextHeadings()) {
			lines = normalizeSetextHeadings(lines);
		}
		if (config.isNormalizeBlockDelimiters()) {
			lines = normalizeBlockDelimiters(lines);
		}
		if (config.isRemoveTrailingHeaderEqualsSign()) {
			lines = removeTrailingHeaderEqualsSign(lines);
		}
		if (config.isTitleCase()) {
			lines = applyTitleCase(lines);
		}
		if (config.isNormalizeListBullets()) {
			lines = normalizeListBullets(lines);
		}
		if (config.isNormalizeOrderedListMarkers()) {
			lines = normalizeOrderedListMarkers(lines);
		}
		if (config.isEnsureHeadingBlankLines()) {
			lines = ensureHeadingBlankLines(lines);
		}
		if (config.isOneSentencePerLine()) {
			lines = applySentencePerLine(lines);
		}

		List<String> result = new ArrayList<>(Arrays.asList(lines));
		if (config.isCollapseConsecutiveBlankLines()) {
			result = collapseBlankLines(result);
		}
		return String.join("\n", result);
	}

	// ── normalizeSetextHeadings ───────────────────────────────────────────────

	private String[] normalizeSetextHeadings(String[] lines) {
		List<String> result = new ArrayList<>(lines.length);
		BlockTracker bt = new BlockTracker();
		int i = 0;
		while (i < lines.length) {
			String line = lines[i];
			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				i++;
				continue;
			}
			if (isBlockDelimiter(line)) {
				result.add(line);
				bt.open(line);
				i++;
				continue;
			}
			if (i + 1 < lines.length) {
				Integer level = detectSetextUnderline(line, lines[i + 1]);
				if (level != null) {
					result.add(ATX_PREFIX[level] + line);
					i += 2;
					continue;
				}
			}
			result.add(line);
			i++;
		}
		return result.toArray(new String[0]);
	}

	/**
	 * Returns the heading level if {@code titleCandidate} + {@code underlineLine}
	 * form a setext-style heading, or {@code null} otherwise.
	 *
	 * <p>Title candidates must be plain prose: lines that begin with structural
	 * AsciiDoc syntax ({@code =}, {@code [}, {@code //}, {@code .}, {@code :},
	 * {@code *}, {@code -}, {@code |}, {@code +}) are never heading titles.
	 */
	private Integer detectSetextUnderline(String titleCandidate, String underlineLine) {
		if (titleCandidate.isEmpty()) {
			return null;
		}
		// Structural AsciiDoc lines are never heading title candidates
		char first = titleCandidate.charAt(0);
		if (first == '=' || first == '[' || first == '.' || first == ':'
				|| first == '*' || first == '-' || first == '|' || first == '+'
				|| titleCandidate.startsWith("//")) {
			return null;
		}
		if (underlineLine.isEmpty()) {
			return null;
		}
		char underlineChar = underlineLine.charAt(0);
		int level;
		switch (underlineChar) {
		case '=':
			level = 0;
			break;
		case '-':
			level = 1;
			break;
		case '~':
			level = 2;
			break;
		case '^':
			level = 3;
			break;
		case '+':
			level = 4;
			break;
		default:
			return null;
		}
		for (int j = 1; j < underlineLine.length(); j++) {
			if (underlineLine.charAt(j) != underlineChar) {
				return null;
			}
		}
		// Underline must be at least as long as the title
		if (underlineLine.length() < titleCandidate.length()) {
			return null;
		}
		return level;
	}

	// ── normalizeBlockDelimiters ──────────────────────────────────────────────

	/**
	 * Shortens over-long block delimiter lines to exactly four characters.
	 *
	 * <p>A line like {@code --------} (eight dashes) becomes {@code ----}.
	 * Lines that are already four characters are left unchanged.  Setext
	 * heading underlines (preceded by a prose title) are also left unchanged.
	 *
	 * <p>A state machine tracks open/close pairs so that only the first
	 * occurrence of a delimiter char on an unmatched line is subject to the
	 * setext heuristic; once a block is open, its closing delimiter is
	 * normalised unconditionally.
	 */
	private String[] normalizeBlockDelimiters(String[] lines) {
		List<String> result = new ArrayList<>(lines.length);
		BlockTracker bt = new BlockTracker();

		for (String line : lines) {
			if (bt.isOpen()) {
				// Inside a block: normalize the closing delimiter; pass everything else through.
				String closed = bt.tryClose(line);
				result.add(closed != null ? closed.repeat(4) : line);
			} else if (isOverLongBlockDelimiter(line)) {
				// Outside a block: decide if this is a setext underline or a block delimiter.
				String prev = result.isEmpty() ? null : result.get(result.size() - 1);
				boolean isSetextUnderline = prev != null && !prev.isBlank()
						&& detectSetextUnderline(prev, line) != null;
				if (isSetextUnderline) {
					result.add(line);
					// Do NOT enter block-tracking state; setext underlines are not block openers.
				} else {
					result.add(String.valueOf(line.charAt(0)).repeat(4));
					bt.open(line);
				}
			} else if (isBlockDelimiter(line)) {
				// A minimal (4-char) delimiter: enter block-tracking state.
				result.add(line);
				bt.open(line);
			} else {
				result.add(line);
			}
		}
		return result.toArray(new String[0]);
	}

	/** True when every character in {@code line} equals {@code c}. */
	private static boolean isAllSameChar(String line, char c) {
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) != c) {
				return false;
			}
		}
		return true;
	}

	/** True when the line is a block-delimiter character repeated five or more times. */
	private static boolean isOverLongBlockDelimiter(String line) {
		return line.length() > 4 && isBlockDelimiter(line);
	}

	// ── removeTrailingHeaderEqualsSign ────────────────────────────────────────

	/**
	 * Normalises ATX heading syntax: removes symmetric trailing {@code =} signs and
	 * collapses any whitespace (including tabs) after the leading {@code =} signs to
	 * a single space.
	 *
	 * <p>Examples: {@code == Title ==} → {@code == Title},
	 * {@code ===\tTitle} → {@code === Title}.
	 */
	private static String[] removeTrailingHeaderEqualsSign(String[] lines) {
		String[] result = new String[lines.length];
		for (int i = 0; i < lines.length; i++) {
			Matcher symmetric = SYMMETRIC_HEADING.matcher(lines[i]);
			if (symmetric.matches()) {
				result[i] = symmetric.group(1) + " " + symmetric.group(2);
				continue;
			}
			// Normalise whitespace (tab → space) in any remaining ATX heading.
			Matcher atx = ATX_HEADING.matcher(lines[i]);
			result[i] = atx.matches() ? atx.group(1) + " " + atx.group(2) : lines[i];
		}
		return result;
	}

	// ── collapseConsecutiveBlankLines ─────────────────────────────────────────

	private static List<String> collapseBlankLines(List<String> lines) {
		List<String> result = new ArrayList<>(lines.size());
		int consecutiveBlank = 0;
		for (String line : lines) {
			if (line.isBlank()) {
				consecutiveBlank++;
				if (consecutiveBlank <= 1) {
					result.add(line);
				}
			} else {
				consecutiveBlank = 0;
				result.add(line);
			}
		}
		return result;
	}

	// ── ensureSourceDelimiters ────────────────────────────────────────────────

	/**
	 * Wraps bare {@code [source,...]} and {@code [listing]} blocks that have no
	 * {@code ----} delimiter with a {@code ----} / {@code ----} pair.
	 *
	 * <p>A block is considered "already delimited" when the line immediately
	 * following the attribute line is any AsciiDoc block-delimiter line.
	 * Content is collected until the first blank line or end of file.
	 */
	private static String[] ensureSourceDelimiters(String[] lines) {
		List<String> result = new ArrayList<>(lines.length + 8);
		BlockTracker bt = new BlockTracker();
		int i = 0;
		while (i < lines.length) {
			String line = lines[i];

			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				i++;
				continue;
			}

			if (isBlockDelimiter(line)) {
				result.add(line);
				bt.open(line);
				i++;
				continue;
			}

			if (SOURCE_BLOCK_ATTR.matcher(line).matches()) {
				result.add(line);
				i++;
				if (i < lines.length) {
					String next = lines[i];
					if (isBlockDelimiter(next)) {
						// Already has a delimiter — enter block state normally
						result.add(next);
						bt.open(next);
						i++;
					} else if (!next.isBlank() && !next.startsWith("[")) {
						// No delimiter: wrap the following paragraph
						result.add("----");
						while (i < lines.length && !lines[i].isBlank()) {
							result.add(lines[i]);
							i++;
						}
						result.add("----");
					}
					// blank or another attribute line: leave as-is
				}
				continue;
			}

			result.add(line);
			i++;
		}
		return result.toArray(new String[0]);
	}

	// ── ensureHeadingBlankLines ───────────────────────────────────────────────

	/**
	 * Ensures every ATX section heading is preceded and followed by a blank line.
	 * Lines inside delimited blocks are not touched.
	 */
	private static String[] ensureHeadingBlankLines(String[] lines) {
		List<String> result = new ArrayList<>(lines.length + 8);
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];

			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				continue;
			}
			if (isBlockDelimiter(line)) {
				result.add(line);
				bt.open(line);
				continue;
			}

			if (SECTION_HEADING.matcher(line).matches()) {
				// blank line before (skip if first line or previous is already blank)
				if (!result.isEmpty() && !result.get(result.size() - 1).isBlank()) {
					result.add("");
				}
				result.add(line);
				// blank line after (skip if last line or next is already blank)
				if (i + 1 < lines.length && !lines[i + 1].isBlank()) {
					result.add("");
				}
			} else {
				result.add(line);
			}
		}
		return result.toArray(new String[0]);
	}

	// ── removeTrailingWhitespace ──────────────────────────────────────────────

	private static String[] removeTrailingWhitespace(String[] lines) {
		String[] result = new String[lines.length];
		for (int i = 0; i < lines.length; i++) {
			result[i] = lines[i].stripTrailing();
		}
		return result;
	}

	// ── processLinesSkippingBlocks ────────────────────────────────────────────

	/**
	 * Applies {@code transform} to every line that is outside a delimited block.
	 * Lines that open or close a block, and all lines between them, are passed
	 * through unchanged.
	 */
	private static String[] processLinesSkippingBlocks(String[] lines, UnaryOperator<String> transform) {
		String[] result = new String[lines.length];
		BlockTracker bt = new BlockTracker();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (bt.isOpen()) {
				result[i] = line;
				bt.tryClose(line);
			} else if (isBlockDelimiter(line)) {
				result[i] = line;
				bt.open(line);
			} else {
				result[i] = transform.apply(line);
			}
		}
		return result;
	}

	// ── normalizeListBullets ──────────────────────────────────────────────────

	/**
	 * Converts dash-style unordered list items ({@code - item}) to the standard
	 * AsciiDoc asterisk style ({@code * item}).  Lines inside delimited blocks
	 * are passed through unchanged.
	 */
	private static String[] normalizeListBullets(String[] lines) {
		return processLinesSkippingBlocks(lines,
				line -> line.startsWith("- ") ? "* " + line.substring(2) : line);
	}

	// ── normalizeOrderedListMarkers ───────────────────────────────────────────

	/**
	 * Converts explicit-number ordered list items ({@code 1. item}) to the
	 * AsciiDoc auto-numbered dot style ({@code . item}).  Lines inside
	 * delimited blocks are passed through unchanged.
	 */
	private static String[] normalizeOrderedListMarkers(String[] lines) {
		return processLinesSkippingBlocks(lines, line -> {
			if (line.isEmpty() || line.charAt(0) < '0' || line.charAt(0) > '9')
				return line;
			int i = 1;
			while (i < line.length() && line.charAt(i) >= '0' && line.charAt(i) <= '9')
				i++;
			if (i + 1 >= line.length() || line.charAt(i) != '.')
				return line;
			char sep = line.charAt(i + 1);
			if (sep != ' ' && sep != '\t')
				return line;
			return ". " + line.substring(i + 2);
		});
	}

	// ── titleCase ─────────────────────────────────────────────────────────────

	private static String[] applyTitleCase(String[] lines) {
		return processLinesSkippingBlocks(lines, AsciidocFormatterFunc::titleCaseLine);
	}

	private static String titleCaseLine(String line) {
		// Section heading: = Title, == Title, ...
		Matcher m = SECTION_HEADING.matcher(line);
		if (m.matches()) {
			return m.group(1) + " " + toTitleCase(m.group(2));
		}
		// Block title: .Title (single dot, not .. and not dot-space)
		if (line.length() > 1 && line.charAt(0) == '.' && line.charAt(1) != '.' && line.charAt(1) != ' ') {
			return "." + toTitleCase(line.substring(1));
		}
		return line;
	}

	private static String toTitleCase(String text) {
		String[] words = text.split(" +", -1); // " +" avoids empty tokens from consecutive spaces
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < words.length; i++) {
			if (i > 0) {
				sb.append(' ');
			}
			boolean forceCapitalize = (i == 0) || (i == words.length - 1);
			sb.append(capitalizeWordForTitle(words[i], forceCapitalize));
		}
		return sb.toString();
	}

	private static String capitalizeWordForTitle(String word, boolean forceCapitalize) {
		if (word.isEmpty()) {
			return word;
		}
		// Skip words containing AsciiDoc special markup (attributes, code spans, block attrs)
		if (word.contains("{") || word.contains("`") || word.contains("[")) {
			return word;
		}
		// Skip AsciiDoc macros (word:target — colon not at end)
		int colonIdx = word.indexOf(':');
		if (colonIdx > 0 && colonIdx < word.length() - 1) {
			return word;
		}
		// Find first letter
		int firstLetter = -1;
		for (int i = 0; i < word.length(); i++) {
			if (Character.isLetter(word.charAt(i))) {
				firstLetter = i;
				break;
			}
		}
		if (firstLetter < 0) {
			return word;
		}
		// Extract only letters from firstLetter onward for lowercase-set membership test
		StringBuilder coreBuilder = new StringBuilder();
		for (int i = firstLetter; i < word.length(); i++) {
			char c = word.charAt(i);
			if (Character.isLetter(c)) {
				coreBuilder.append(Character.toLowerCase(c));
			}
		}
		String core = coreBuilder.toString();
		if (!forceCapitalize && TITLE_CASE_LOWERCASE.contains(core)) {
			return word.toLowerCase(Locale.ROOT);
		}
		// Uppercase first letter, leave the rest unchanged (preserves acronyms like API)
		return word.substring(0, firstLetter)
				+ Character.toUpperCase(word.charAt(firstLetter))
				+ word.substring(firstLetter + 1);
	}

	// ── oneSentencePerLine ────────────────────────────────────────────────────

	private String[] applySentencePerLine(String[] lines) {
		List<String> result = new ArrayList<>(lines.length);
		List<String> paragraphBuffer = new ArrayList<>();
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];

			// ── inside a delimited block: pass through until matching closing delimiter
			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				continue;
			}

			// ── opening delimiter: flush any accumulated paragraph, enter block
			if (isBlockDelimiter(line)) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				bt.open(line);
				continue;
			}

			// ── setext heading pair: lookahead to avoid mangling title + underline.
			// Use detectSetextUnderline (same logic as normalizeSetextHeadings) so that
			// structural lines like [source,java] or short dash-lines like -- are not
			// mistakenly treated as heading pairs.
			if (i + 1 < lines.length && detectSetextUnderline(line, lines[i + 1]) != null) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				result.add(lines[i + 1]);
				i++;
				continue;
			}

			// ── blank or structurally special line: flush paragraph, pass through
			if (line.isBlank() || isSpecialLine(line)) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				continue;
			}

			// ── plain paragraph text: accumulate
			paragraphBuffer.add(line);
		}

		flushParagraph(paragraphBuffer, result);
		return result.toArray(new String[0]);
	}

	private static void flushParagraph(List<String> buffer, List<String> result) {
		if (buffer.isEmpty()) {
			return;
		}
		String joined = MULTI_WHITESPACE.matcher(String.join(" ", buffer)).replaceAll(" ").trim();
		result.addAll(splitIntoSentences(joined));
		buffer.clear();
	}

	private static boolean isBlockDelimiter(String line) {
		int len = line.length();
		if (len < 4)
			return false;
		char c = line.charAt(0);
		if (BLOCK_DELIMITER_CHARS.indexOf(c) < 0)
			return false;
		for (int i = 1; i < len; i++) {
			if (line.charAt(i) != c)
				return false;
		}
		return true;
	}

	/**
		 * Returns true for lines that are structural AsciiDoc syntax and must be
		 * emitted verbatim rather than accumulated into a paragraph.
		 */
	private static boolean isSpecialLine(String line) {
		if (line.isEmpty())
			return false;
		char first = line.charAt(0);
		if (first == '=')
			return true;           // headings: = Title, == Section …
		if (first == '[')
			return true;            // block attributes: [source,java]
		if (line.startsWith("//"))
			return true;   // line or block comments
		// attribute entries  :attr: value  (but not :: description-list markers)
		if (first == ':' && line.length() > 1 && line.charAt(1) != ':')
			return true;
		if (first == '|')
			return true;            // table cells
		if (line.equals("+"))
			return true;        // list continuation
		if (first == ' ' || first == '\t')
			return true; // indented literal paragraph
		// unordered list items: * item, ** item, - item, -- item, …
		if (first == '*' || first == '-') {
			int i = 1;
			while (i < line.length() && line.charAt(i) == first)
				i++;
			return i < line.length() && line.charAt(i) == ' ';
		}
		// ordered list items (AsciiDoc auto-number): . item, .. item, …
		if (first == '.') {
			int i = 1;
			while (i < line.length() && line.charAt(i) == '.')
				i++;
			return i < line.length() && line.charAt(i) == ' ';
		}
		// explicit numbered list items: 1. item, 42.\titem
		if (first >= '0' && first <= '9') {
			int i = 1;
			while (i < line.length() && line.charAt(i) >= '0' && line.charAt(i) <= '9')
				i++;
			return i + 1 < line.length() && line.charAt(i) == '.'
					&& (line.charAt(i + 1) == ' ' || line.charAt(i + 1) == '\t');
		}
		if (line.startsWith("<<<"))
			return true;  // page break
		if (line.equals("'''"))
			return true;       // horizontal rule (thematic break)
		// block macros (include::, toc::, image::, …) and description-list terms (term::)
		return isBlockMacroOrTerm(line);
	}

	/** True when {@code line} is a block-macro call or description-list term ({@code word::…}). */
	private static boolean isBlockMacroOrTerm(String line) {
		int len = line.length();
		int i = 0;
		while (i < len) {
			char c = line.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || (c >= '0' && c <= '9')) {
				i++;
			} else {
				break;
			}
		}
		return i > 0 && i + 1 < len && line.charAt(i) == ':' && line.charAt(i + 1) == ':';
	}

	// ── sentence splitting ────────────────────────────────────────────────────

	private static List<String> splitIntoSentences(String text) {
		if (text.isEmpty()) {
			return Collections.emptyList();
		}

		List<String> sentences = new ArrayList<>();
		int start = 0;
		int i = 0;

		while (i < text.length()) {
			char c = text.charAt(i);

			if (c == '.' || c == '!' || c == '?') {

				// Skip ellipsis (two or more consecutive dots)
				if (c == '.' && i + 1 < text.length() && text.charAt(i + 1) == '.') {
					i++;
					while (i < text.length() && text.charAt(i) == '.') {
						i++;
					}
					continue;
				}

				// Abbreviations: only relevant for full stops
				if (c == '.' && isAbbreviationContext(text, i)) {
					i++;
					continue;
				}

				// Skip optional closing characters after the punctuation mark
				int j = i + 1;
				while (j < text.length() && isSentenceClosingChar(text.charAt(j))) {
					j++;
				}

				// End of string — remaining text collected after the loop
				if (j >= text.length()) {
					i = j;
					continue;
				}

				// Sentence boundary: whitespace followed by an uppercase letter (or end)
				if (Character.isWhitespace(text.charAt(j))) {
					int k = j;
					while (k < text.length() && Character.isWhitespace(text.charAt(k))) {
						k++;
					}
					if (k >= text.length() || Character.isUpperCase(text.charAt(k)) || Character.isDigit(text.charAt(k))) {
						String sentence = text.substring(start, j).trim();
						if (!sentence.isEmpty()) {
							sentences.add(sentence);
						}
						start = k;
						i = k;
						continue;
					}
				}
			}

			i++;
		}

		String remaining = text.substring(start).trim();
		if (!remaining.isEmpty()) {
			sentences.add(remaining);
		}
		return sentences;
	}

	private static boolean isAbbreviationContext(String text, int dotPos) {
		// Digit before dot: decimal numbers and numbered items such as "section 1.2."
		if (dotPos > 0 && Character.isDigit(text.charAt(dotPos - 1))) {
			return true;
		}
		// Extract the alphabetic word immediately before the dot
		int wordEnd = dotPos;
		int wordStart = wordEnd - 1;
		while (wordStart >= 0 && Character.isLetter(text.charAt(wordStart))) {
			wordStart--;
		}
		wordStart++;
		if (wordStart >= wordEnd) {
			return false;
		}
		String word = text.substring(wordStart, wordEnd);
		// Single lowercase letter covers components of e.g., i.e., a.k.a., etc.
		if (word.length() == 1 && Character.isLowerCase(word.charAt(0))) {
			return true;
		}
		return ABBREVIATIONS.contains(word.toLowerCase(Locale.ROOT));
	}

	private static boolean isSentenceClosingChar(char c) {
		return c == ')' || c == ']' || c == '"' || c == '\''
				|| c == '\u2019' /* right single quotation mark */
				|| c == '\u201D' /* right double quotation mark */;
	}

	// ── BlockTracker ──────────────────────────────────────────────────────────

	/**
	 * Tracks the open/close state of a single AsciiDoc delimited block across a
	 * line scan.  All block-aware methods share this class instead of each
	 * maintaining their own {@code openDelimiterChar} variable.
	 */
	private static final class BlockTracker {
		private char delimChar = '\0';

		boolean isOpen() {
			return delimChar != '\0';
		}

		void open(String line) {
			delimChar = line.charAt(0);
		}

		/**
		 * Tests whether {@code line} closes the currently open block.
		 * If so, resets the open state and returns the delimiter character;
		 * returns {@code null} if the block remains open.
		 */
		String tryClose(String line) {
			if (delimChar != '\0' && line.length() >= 4 && isAllSameChar(line, delimChar)) {
				String closed = String.valueOf(delimChar);
				delimChar = '\0';
				return closed;
			}
			return null;
		}
	}
}
