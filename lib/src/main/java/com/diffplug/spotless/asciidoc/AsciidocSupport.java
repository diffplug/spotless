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
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import edu.umd.cs.findbugs.annotations.Nullable;

/** Shared utilities and constants for Asciidoc formatting. */
final class AsciidocSupport {
	private AsciidocSupport() {}

	private static final String BLOCK_DELIMITER_CHARS = "-=.*_+/";

	static final Pattern MULTI_WHITESPACE = Pattern.compile("\\s+");

	static void removeTrailingWhitespace(List<String> lines) {
		lines.replaceAll(String::stripTrailing);
	}

	static void collapseBlankLines(List<String> lines) {
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

	static boolean isBlockDelimiter(CharSequence line) {
		int len = line.length();
		if (len < 4) {
			return false;
		}
		char c = line.charAt(0);
		return BLOCK_DELIMITER_CHARS.indexOf(c) >= 0 && IntStream.range(1, len).noneMatch(i -> line.charAt(i) != c);
	}

	static boolean isAllSameChar(CharSequence line, char c) {
		return IntStream.range(0, line.length()).noneMatch(i -> line.charAt(i) != c);
	}

	@Nullable static Integer detectSetextUnderline(String titleCandidate, CharSequence underlineLine) {
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

	static boolean isSpecialLine(String line) {
		if (line.isEmpty()) {
			return false;
		}
		char first = line.charAt(0);
		if (first == '=' || first == '[' || first == '|' || first == ' ' || first == '\t') {
			return true;
		}
		if (line.startsWith("//") || line.startsWith("<<<") || "'''".equals(line) || "+".equals(line)) {
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
			// Treat list items as special lines
			if (line.length() > 1 && line.charAt(1) == ' ') {
				return true;
			}
			int i = 1;
			while (i < line.length() && line.charAt(i) == first) {
				i++;
			}
			return i == line.length() && i >= 3 || i < line.length() && line.charAt(i) == ' '; // Horizontal rule (--- or ***)
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

	private static boolean isBlockMacroOrTerm(CharSequence line) {
		int len = line.length();
		int i = 0;
		while (i < len) {
			char c = line.charAt(i);
			if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_' || c >= '0' && c <= '9') {
				i++;
			} else {
				break;
			}
		}
		return i > 0 && i + 1 < len && line.charAt(i) == ':' && line.charAt(i + 1) == ':';
	}

}
