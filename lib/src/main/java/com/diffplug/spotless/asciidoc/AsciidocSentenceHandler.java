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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Handles splitting text into one sentence per line. */
final class AsciidocSentenceHandler {
	private final List<String> lines;

	AsciidocSentenceHandler(List<String> lines) {
		this.lines = lines;
	}

	// Known abbreviations that end with a period but do not end a sentence
	private static final Set<String> ABBREVIATIONS = Set.of(
			"mr", "mrs", "ms", "dr", "prof", "sr", "jr",
			"vs", "etc", "approx", "dept", "fig", "no", "vol",
			"ch", "sec", "ref", "rev", "st", "mt", "ft",
			"ave", "blvd", "rd", "pp", "al", "ed", "eds",
			"corp", "inc", "ltd", "llc",
			"jan", "feb", "mar", "apr", "jun", "jul",
			"aug", "sep", "sept", "oct", "nov", "dec",
			"bspw", "bzw", "bzgl", "ca", "evtl", "exkl", "inkl", "sog");

	void applySentencePerLine() {
		Collection<String> result = new ArrayList<>(lines.size());
		Collection<String> paragraphBuffer = new ArrayList<>();
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				continue;
			}

			if (AsciidocSupport.isBlockDelimiter(line)) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				bt.open(line);
				continue;
			}

			if (i + 1 < lines.size() && AsciidocSupport.detectSetextUnderline(line, lines.get(i + 1)) != null) {
				flushParagraph(paragraphBuffer, result);
				result.add(line);
				result.add(lines.get(i + 1));
				i++;
				continue;
			}

			if (line.isBlank() || AsciidocSupport.isSpecialLine(line)) {
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

	private static void flushParagraph(Collection<String> buffer, Collection<String> result) {
		if (buffer.isEmpty()) {
			return;
		}
		String joined = AsciidocSupport.MULTI_WHITESPACE.matcher(String.join(" ", buffer)).replaceAll(" ").trim();
		result.addAll(splitIntoSentences(joined));
		buffer.clear();
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
		int wordStart = dotPos - 1;
		while (wordStart >= 0 && Character.isLetter(text.charAt(wordStart))) {
			wordStart--;
		}
		wordStart++;
		if (wordStart >= dotPos) {
			return false;
		}
		String word = text.substring(wordStart, dotPos);
		return word.length() == 1 || ABBREVIATIONS.contains(word.toLowerCase(Locale.ROOT)); // Initials (e.g., A. Smith)
	}

	private static boolean isSentenceClosingChar(char c) {
		return c == ')' || c == ']' || c == '"' || c == '\''
				|| c == '\u2019'
				|| c == '\u201D';
	}
}
