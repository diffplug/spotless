/*
 * Copyright 2025-2026 DiffPlug
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
package com.diffplug.spotless.java;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Lint;

/** Forbids any wildcard import statements. */
public final class ForbidWildcardImportsStep {

	/**
	 * Matches lines like 'import foo.*;' or 'import static foo.*;'.
	 */
	private static final String REGEX = "(?m)^[ \\t]*import\\s+(?:static\\s+)?[^;\\n]*\\*;\\R?";
	private static final String NAME = "forbidWildcardImports";
	private static final String ERROR = "Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this";

	private ForbidWildcardImportsStep() {}

	public static FormatterStep create() {
		return FormatterStep.create(NAME, Pattern.compile(REGEX, Pattern.UNIX_LINES | Pattern.MULTILINE), ForbidWildcardImportsStep::toFormatter);
	}

	private static FormatterFunc toFormatter(Pattern regex) {
		return new FormatterFunc() {
			@Override
			public String apply(String raw) {
				return raw;
			}

			@Override
			public List<Lint> lint(String raw, File file) {
				var matcher = regex.matcher(raw);
				if (!matcher.find()) {
					return List.of();
				}
				List<Lint> lints = new ArrayList<>();
				LexicalInput input = new LexicalInput(raw);
				int scanOffset = 0;
				int lineOffset = 0;
				int line = 1;
				do {
					// A skipped literal or comment may extend past this apparent import.
					while (input.rawOffset(scanOffset) < matcher.start()) {
						scanOffset = skipLiteralOrComment(input.text, scanOffset);
					}
					if (input.rawOffset(scanOffset) > matcher.start()) {
						continue;
					}
					while (lineOffset < matcher.start()) {
						if (raw.charAt(lineOffset++) == '\n') {
							++line;
						}
					}
					String errorCode = matcher.group().trim();
					int firstNewline = errorCode.indexOf('\n');
					if (firstNewline != -1) {
						errorCode = errorCode.substring(0, firstNewline);
					}
					lints.add(Lint.atLine(line, errorCode, ERROR));
				} while (matcher.find());
				return lints;
			}
		};
	}

	/** Skips a complete literal or comment, or advances one character through ordinary code. */
	private static int skipLiteralOrComment(String raw, int offset) {
		if (raw.startsWith("//", offset)) {
			int end = raw.indexOf('\n', offset + 2);
			return end == -1 ? raw.length() : end;
		}
		if (raw.startsWith("/*", offset)) {
			int end = raw.indexOf("*/", offset + 2);
			return end == -1 ? raw.length() : end + 2;
		}
		char c = raw.charAt(offset);
		if (c == '"' || c == '\'') {
			String delimiter = raw.startsWith("\"\"\"", offset) ? "\"\"\"" : String.valueOf(c);
			int end = offset + delimiter.length();
			while (end < raw.length()) {
				if (raw.charAt(end) == '\\') {
					// An escaped quote cannot close a literal or text block.
					end += 2;
				} else if (raw.startsWith(delimiter, end)) {
					return end + delimiter.length();
				} else {
					++end;
				}
			}
			return raw.length();
		}
		return offset + 1;
	}

	/** Translates Unicode escapes for lexing, while retaining raw offsets for diagnostics. */
	private static final class LexicalInput {
		private final String text;
		private final int[] rawOffsets;

		LexicalInput(String raw) {
			if (!raw.contains("\\u")) {
				text = raw;
				rawOffsets = new int[0];
				return;
			}
			StringBuilder translated = new StringBuilder(raw.length());
			rawOffsets = new int[raw.length() + 1];
			int backslashes = 0;
			boolean previousWasEscape = false;
			for (int offset = 0; offset < raw.length();) {
				char c = raw.charAt(offset);
				int next = offset + 1;
				boolean isEscape = false;
				// JLS 3.3: eligibility depends on the translated backslashes, not just the raw input.
				if (c == '\\' && (previousWasEscape || backslashes % 2 == 0)) {
					int digits = next;
					while (digits < raw.length() && raw.charAt(digits) == 'u') {
						++digits;
					}
					if (digits > next && digits + 4 <= raw.length()) {
						int value = 0;
						for (int i = digits; i < digits + 4; ++i) {
							char digit = raw.charAt(i);
							int hex = Character.digit(digit, 16);
							if (digit > 127 || hex == -1) {
								value = -1;
								break;
							}
							value = value * 16 + hex;
						}
						if (value != -1) {
							c = (char) value;
							next = digits + 4;
							isEscape = true;
						}
					}
				}
				rawOffsets[translated.length()] = offset;
				// Unicode escapes can introduce either kind of Java line terminator.
				translated.append(c == '\r' ? '\n' : c);
				backslashes = c == '\\' ? backslashes + 1 : 0;
				previousWasEscape = isEscape;
				offset = next;
			}
			rawOffsets[translated.length()] = raw.length();
			text = translated.toString();
		}

		int rawOffset(int offset) {
			return rawOffsets.length == 0 ? offset : rawOffsets[offset];
		}
	}
}
