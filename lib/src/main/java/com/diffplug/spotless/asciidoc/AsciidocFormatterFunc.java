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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

public class AsciidocFormatterFunc implements FormatterFunc {

	// Single source of truth for block-delimiter characters; isBlockDelimiter is derived from this.
	// To add a new delimiter type, append its character here only.
	private static final String BLOCK_DELIMITER_CHARS = "-=.*_+/";

	// Heading with trailing = signs: == Title == or === Title ===
	// Captured groups: (1) leading equals, (2) title text (trimmed)
	private static final Pattern SYMMETRIC_HEADING = Pattern.compile("^(={1,6})\\s+(.*\\S)\\s+=+\\s*$");

	// Section heading: = Title or == Title, etc.
	// Captured groups: (1) leading equals, (2) trimmed title text
	private static final Pattern SECTION_HEADING = Pattern.compile("^(={1,6})\\s+(\\S.*?)\\s*$");

	// Words lowercased in title case (articles, conjunctions, short prepositions)
	private static final Set<String> TITLE_CASE_LOWERCASE = Set.of(
			"a", "an", "the",
			"and", "but", "or", "nor", "for", "yet", "so",
			"at", "by", "in", "of", "on", "to", "up", "as", "off", "out", "per", "via", "from", "with");

	// Source / listing block attribute lines: [source], [source,java], [listing], [source%linenums,java], [source#id,java], etc.
	private static final Pattern SOURCE_BLOCK_ATTR = Pattern.compile("^\\[(source|listing)[,\\]%#].*");

	// ATX heading prefixes for setext -> ATX conversion: ATX_PREFIX[n] = "=".repeat(n+1) + " "
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

	@NonNull @Override
	public String apply(@NonNull String input) throws Exception {
		// Use \R to match any line break (LF, CRLF, CR) and avoid multiple replacements
		List<String> lines = new ArrayList<>(Arrays.asList(Pattern.compile("\\R").split(input, -1)));

		// Ordering constraints:
		//   removeTrailingWhitespace  before  collapseConsecutiveBlankLines
		//     - whitespace-only lines must be emptied before they can be collapsed.
		//   normalizeSetextHeadings   before  ensureHeadingBlankLines
		//     - setext headings are converted to ATX first so they receive blank-line padding.
		if (config.isRemoveTrailingWhitespace()) {
			removeTrailingWhitespace(lines);
		}
		if (config.isEnsureSourceDelimiters()) {
			ensureSourceDelimiters(lines);
		}
		if (config.isNormalizeSetextHeadings()) {
			normalizeSetextHeadings(lines);
		}
		if (config.isNormalizeBlockDelimiters()) {
			normalizeBlockDelimiters(lines);
		}
		if (config.isRemoveTrailingHeaderEqualsSign()) {
			removeTrailingHeaderEqualsSign(lines);
		}

		// Combine simple line-by-line transforms into a single in-place pass
		if (config.isTitleCase() || config.isNormalizeListBullets() || config.isNormalizeOrderedListMarkers()) {
			applyLineTransformations(lines);
		}

		if (config.isEnsureHeadingBlankLines()) {
			ensureHeadingBlankLines(lines);
		}
		if (config.isOneSentencePerLine()) {
			applySentencePerLine(lines);
		}
		if (config.isCollapseConsecutiveBlankLines()) {
			collapseBlankLines(lines);
		}

		return String.join("\n", lines);
	}

	/**
	 * Converts setext-style headings to ATX-style headings.
	 * Performs the transformation in-place on the input list.
	 */
	private static void normalizeSetextHeadings(List<String> lines) {
		BlockTracker bt = new BlockTracker();
		int readIdx = 0;
		int writeIdx = 0;
		while (readIdx < lines.size()) {
			String line = lines.get(readIdx);
			if (bt.isOpen()) {
				lines.set(writeIdx++, line);
				bt.tryClose(line);
				readIdx++;
				continue;
			}
			if (isBlockDelimiter(line)) {
				lines.set(writeIdx++, line);
				bt.open(line);
				readIdx++;
				continue;
			}
			if (readIdx + 1 < lines.size()) {
				Integer level = detectSetextUnderline(line, lines.get(readIdx + 1));
				if (level != null) {
					lines.set(writeIdx++, ATX_PREFIX[level] + line);
					readIdx += 2;
					continue;
				}
			}
			lines.set(writeIdx++, line);
			readIdx++;
		}
		if (writeIdx < lines.size()) {
			lines.subList(writeIdx, lines.size()).clear();
		}
	}

	/**
	 * Returns the heading level if {@code titleCandidate} + {@code underlineLine}
	 * form a setext-style heading, or {@code null} otherwise.
	 */
	@Nullable private static Integer detectSetextUnderline(String titleCandidate, CharSequence underlineLine) {
		if (titleCandidate.isEmpty()) {
			return null;
		}
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
		if (underlineLine.length() < titleCandidate.length()) {
			return null;
		}
		return level;
	}

	private static void normalizeBlockDelimiters(List<String> lines) {
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (bt.isOpen()) {
				String closed = bt.tryClose(line);
				if (closed != null) {
					lines.set(i, closed.repeat(4));
				}
			} else if (isOverLongBlockDelimiter(line)) {
				String prev = i == 0 ? null : lines.get(i - 1);
				boolean isSetextUnderline = prev != null && !prev.isBlank()
						&& detectSetextUnderline(prev, line) != null;
				if (!isSetextUnderline) {
					lines.set(i, String.valueOf(line.charAt(0)).repeat(4));
					bt.open(line);
				}
			} else if (isBlockDelimiter(line)) {
				bt.open(line);
			}
		}
	}

	private static boolean isAllSameChar(String line, char c) {
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) != c) {
				return false;
			}
		}
		return true;
	}

	private static boolean isOverLongBlockDelimiter(String line) {
		return line.length() > 4 && isBlockDelimiter(line);
	}

	private static void removeTrailingHeaderEqualsSign(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			Matcher symmetric = SYMMETRIC_HEADING.matcher(line);
			if (symmetric.matches()) {
				lines.set(i, symmetric.group(1) + " " + symmetric.group(2));
				continue;
			}
			Matcher section = SECTION_HEADING.matcher(line);
			if (section.matches()) {
				lines.set(i, section.group(1) + " " + section.group(2));
			}
		}
	}

	/**
	 * Collapses multiple consecutive blank lines into a single blank line.
	 * Performs the transformation in-place on the input list.
	 */
	private static void collapseBlankLines(List<String> lines) {
		int writeIdx = 0;
		int consecutiveBlank = 0;
		for (int readIdx = 0; readIdx < lines.size(); readIdx++) {
			String line = lines.get(readIdx);
			if (line.isBlank()) {
				consecutiveBlank++;
				if (consecutiveBlank <= 1) {
					lines.set(writeIdx++, line);
				}
			} else {
				consecutiveBlank = 0;
				lines.set(writeIdx++, line);
			}
		}
		if (writeIdx < lines.size()) {
			lines.subList(writeIdx, lines.size()).clear();
		}
	}

	private static void ensureSourceDelimiters(List<String> lines) {
		List<String> result = new ArrayList<>(lines.size() + 8);
		BlockTracker bt = new BlockTracker();
		int i = 0;
		while (i < lines.size()) {
			String line = lines.get(i);

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
				if (i < lines.size()) {
					String next = lines.get(i);
					if (isBlockDelimiter(next)) {
						result.add(next);
						bt.open(next);
						i++;
					} else if (!next.isBlank() && !next.startsWith("[")) {
						result.add("----");
						while (i < lines.size() && !lines.get(i).isBlank()) {
							result.add(lines.get(i));
							i++;
						}
						result.add("----");
					}
				}
				continue;
			}

			result.add(line);
			i++;
		}
		lines.clear();
		lines.addAll(result);
	}

	private static void ensureHeadingBlankLines(List<String> lines) {
		List<String> result = new ArrayList<>(lines.size() + 8);
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

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
				if (!result.isEmpty() && !result.get(result.size() - 1).isBlank()) {
					result.add("");
				}
				result.add(line);
				if (i + 1 < lines.size() && !lines.get(i + 1).isBlank()) {
					result.add("");
				}
			} else {
				result.add(line);
			}
		}
		lines.clear();
		lines.addAll(result);
	}

	private static void removeTrailingWhitespace(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			lines.set(i, lines.get(i).stripTrailing());
		}
	}

	private void applyLineTransformations(List<String> lines) {
		BlockTracker bt = new BlockTracker();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (bt.isOpen()) {
				bt.tryClose(line);
			} else if (isBlockDelimiter(line)) {
				bt.open(line);
			} else {
				if (config.isTitleCase()) {
					line = titleCaseLine(line);
				}
				if (config.isNormalizeListBullets() && line.startsWith("- ")) {
					line = "* " + line.substring(2);
				}
				if (config.isNormalizeOrderedListMarkers()) {
					line = normalizeOrderedListMarker(line);
				}
				lines.set(i, line);
			}
		}
	}

	private static String normalizeOrderedListMarker(String line) {
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
	}

	private static String titleCaseLine(String line) {
		Matcher m = SECTION_HEADING.matcher(line);
		if (m.matches()) {
			return m.group(1) + " " + toTitleCase(m.group(2));
		}
		if (line.length() > 1 && line.charAt(0) == '.' && line.charAt(1) != '.' && line.charAt(1) != ' ') {
			return "." + toTitleCase(line.substring(1));
		}
		return line;
	}

	private static String toTitleCase(String text) {
		String[] words = text.split(" +", -1);
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
		if (word.contains("{") || word.contains("`") || word.contains("[")) {
			return word;
		}
		int colonIdx = word.indexOf(':');
		if (colonIdx > 0 && colonIdx < word.length() - 1) {
			return word;
		}
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
		return word.substring(0, firstLetter)
				+ Character.toUpperCase(word.charAt(firstLetter))
				+ word.substring(firstLetter + 1);
	}

	private static void applySentencePerLine(List<String> lines) {
		List<String> result = new ArrayList<>(lines.size());
		List<String> paragraphBuffer = new ArrayList<>();
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				continue;
			}

			if (isBlockDelimiter(line)) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				bt.open(line);
				continue;
			}

			if (i + 1 < lines.size() && detectSetextUnderline(line, lines.get(i + 1)) != null) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				result.add(lines.get(i + 1));
				i++;
				continue;
			}

			if (line.isBlank() || isSpecialLine(line)) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				continue;
			}

			paragraphBuffer.add(line);
		}

		flushParagraph(paragraphBuffer, result);
		lines.clear();
		lines.addAll(result);
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

	private static boolean isSpecialLine(String line) {
		if (line.isEmpty()) {
			return false;
		}
		char first = line.charAt(0);
		if (first == '=' || first == '[' || first == '|' || first == ' ' || first == '\t') {
			return true;
		}
		if (line.startsWith("//") || line.startsWith("<<<") || line.equals("'''") || line.equals("+")) {
			return true;
		}
		if (first == ':' && line.length() > 1 && line.charAt(1) != ':') {
			return true;
		}
		if (first == '.' || first == '*' || first == '-') {
			if (line.length() > 1 && line.charAt(1) != first && line.charAt(1) != ' ') {
				if (first == '.') {
					return true; // Block title (.Title)
				}
			}
			int i = 1;
			while (i < line.length() && line.charAt(i) == first) {
				i++;
			}
			if (i == line.length() && i >= 3) {
				return true; // Horizontal rule (--- or ***)
			}
			return i < line.length() && line.charAt(i) == ' ';
		}
		if (Character.isDigit(first)) {
			int i = 1;
			while (i < line.length() && Character.isDigit(line.charAt(i))) {
				i++;
			}
			return i + 1 < line.length() && line.charAt(i) == '.'
					&& (line.charAt(i + 1) == ' ' || line.charAt(i + 1) == '\t');
		}
		return isBlockMacroOrTerm(line);
	}

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

				if (c == '.' && i + 1 < text.length() && text.charAt(i + 1) == '.') {
					i++;
					while (i < text.length() && text.charAt(i) == '.') {
						i++;
					}
					continue;
				}

				if (c == '.' && isAbbreviationContext(text, i)) {
					i++;
					continue;
				}

				int j = i + 1;
				while (j < text.length() && isSentenceClosingChar(text.charAt(j))) {
					j++;
				}

				if (j >= text.length()) {
					i = j;
					continue;
				}

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
		if (dotPos > 0 && Character.isDigit(text.charAt(dotPos - 1))) {
			return true;
		}
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
		if (word.length() == 1) {
			return true; // Initials (e.g., A. Smith)
		}
		return ABBREVIATIONS.contains(word.toLowerCase(Locale.ROOT));
	}

	private static boolean isSentenceClosingChar(char c) {
		return c == ')' || c == ']' || c == '"' || c == '\''
				|| c == '\u2019'
				|| c == '\u201D';
	}

	private static final class BlockTracker {
		private char delimChar = '\0';

		boolean isOpen() {
			return delimChar != '\0';
		}

		void open(String line) {
			delimChar = line.charAt(0);
		}

		@Nullable String tryClose(String line) {
			if (delimChar != '\0' && line.length() >= 4 && isAllSameChar(line, delimChar)) {
				String closed = String.valueOf(delimChar);
				delimChar = '\0';
				return closed;
			}
			return null;
		}
	}
}
