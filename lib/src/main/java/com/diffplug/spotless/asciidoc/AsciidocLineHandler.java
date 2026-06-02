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

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/** Handles line-level transformations for Asciidoc (title case, lists). */
final class AsciidocLineHandler {
	private static final Pattern MULTIPLE_SPACES = Pattern.compile(" +");

	private AsciidocLineHandler() {}

	// Words lowercased in title case (articles, conjunctions, short prepositions)
	private static final Set<String> TITLE_CASE_LOWERCASE = Set.of(
			"a", "an", "the", "and", "but", "or", "nor", "for", "yet", "so", "at", "by", "in", "of",
			"on", "to", "up", "as", "off", "out", "per", "via", "from", "with");

	static void applyLineTransformations(List<String> lines, AsciidocFormatterConfig config) {
		BlockTracker bt = new BlockTracker();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (bt.isOpen()) {
				bt.tryClose(line);
			} else if (AsciidocSupport.isBlockDelimiter(line)) {
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
		if (line.isEmpty() || line.charAt(0) < '0' || line.charAt(0) > '9') {
			return line;
		}
		int i = 1;
		while (i < line.length() && line.charAt(i) >= '0' && line.charAt(i) <= '9') {
			i++;
		}
		if (i + 1 >= line.length() || line.charAt(i) != '.') {
			return line;
		}
		char sep = line.charAt(i + 1);
		if (sep != ' ' && sep != '\t') {
			return line;
		}
		return ". " + line.substring(i + 2);
	}

	private static String titleCaseLine(String line) {
		Matcher matcher = AsciidocHeadingHandler.SECTION_HEADING.matcher(line);
		if (matcher.matches()) {
			return matcher.group(1) + ' ' + toTitleCase(matcher.group(2));
		}
		if (line.length() > 1
				&& line.charAt(0) == '.'
				&& line.charAt(1) != '.'
				&& line.charAt(1) != ' ') {
			return '.' + toTitleCase(line.substring(1));
		}
		return line;
	}

	private static String toTitleCase(CharSequence text) {
		String[] words = MULTIPLE_SPACES.split(text, -1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < words.length; i++) {
			if (i > 0) {
				sb.append(' ');
			}
			boolean forceCapitalize = i == 0 || i == words.length - 1;
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
		int firstLetter = IntStream.range(0, word.length())
				.filter(i -> Character.isLetter(word.charAt(i)))
				.findFirst()
				.orElse(-1);
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
}
