/*
 * Copyright 2023-2026 DiffPlug
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
package com.diffplug.spotless.groovy;

import java.io.Serial;
import java.io.Serializable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

/**
 * Removes unnecessary semicolons from Groovy code. Preserves semicolons inside strings and comments.
 *
 * @author Jose Luis Badano
 */
public final class RemoveSemicolonsStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
	private static final String NAME = "Remove unnecessary semicolons";

	private RemoveSemicolonsStep() {
		// do not instantiate
	}

	public static FormatterStep create() {
		return FormatterStep.create(NAME,
				new State(),
				State::toFormatter);
	}

	private static final class State implements Serializable {
		@Serial
		private static final long serialVersionUID = 1L;

		FormatterFunc toFormatter() {
			return raw -> {
				StringBuilder result = new StringBuilder(raw.length());

				// State tracking
				boolean inSingleQuoteString = false;
				boolean inDoubleQuoteString = false;
				boolean inTripleSingleQuoteString = false;
				boolean inTripleDoubleQuoteString = false;
				boolean inSingleLineComment = false;
				boolean inMultiLineComment = false;
				boolean escaped = false;

				for (int i = 0; i < raw.length(); i++) {
					char c = raw.charAt(i);

					// Check for triple quotes first (needs lookahead)
					if (!inSingleLineComment && !inMultiLineComment && i + 2 < raw.length()) {
						String triple = raw.substring(i, i + 3);
						if ("'''".equals(triple) && !inDoubleQuoteString && !inTripleDoubleQuoteString) {
							inTripleSingleQuoteString = !inTripleSingleQuoteString;
							result.append(triple);
							i += 2;
							continue;
						} else if ("\"\"\"".equals(triple) && !inSingleQuoteString && !inTripleSingleQuoteString) {
							inTripleDoubleQuoteString = !inTripleDoubleQuoteString;
							result.append(triple);
							i += 2;
							continue;
						}
					}

					// Handle escaping
					if (c == '\\' && (inSingleQuoteString || inDoubleQuoteString ||
							inTripleSingleQuoteString || inTripleDoubleQuoteString)) {
						escaped = !escaped;
						result.append(c);
						continue;
					}

					// Check for comments (only if not in string)
					if (!inSingleQuoteString && !inDoubleQuoteString &&
							!inTripleSingleQuoteString && !inTripleDoubleQuoteString && !escaped) {

						// Single line comment
						if (c == '/' && i + 1 < raw.length() && raw.charAt(i + 1) == '/' && !inMultiLineComment) {
							inSingleLineComment = true;
						}
						// Multi-line comment start
						else if (c == '/' && i + 1 < raw.length() && raw.charAt(i + 1) == '*' && !inSingleLineComment) {
							inMultiLineComment = true;
						}
						// Multi-line comment end
						else if (c == '*' && i + 1 < raw.length() && raw.charAt(i + 1) == '/' && inMultiLineComment) {
							inMultiLineComment = false;
							result.append(c);
							if (i + 1 < raw.length()) {
								result.append(raw.charAt(i + 1));
								i++;
							}
							continue;
						}
					}

					// Check for string quotes (only if not in comment and not already in triple quotes)
					if (!inSingleLineComment && !inMultiLineComment && !escaped) {
						if (c == '\'' && !inDoubleQuoteString && !inTripleSingleQuoteString && !inTripleDoubleQuoteString) {
							inSingleQuoteString = !inSingleQuoteString;
						} else if (c == '"' && !inSingleQuoteString && !inTripleSingleQuoteString && !inTripleDoubleQuoteString) {
							inDoubleQuoteString = !inDoubleQuoteString;
						}
					}

					// End single line comment on newline
					if ((c == '\n' || c == '\r') && inSingleLineComment) {
						inSingleLineComment = false;
					}

					// Check if we should remove this semicolon
					if (c == ';' && !inSingleQuoteString && !inDoubleQuoteString &&
							!inTripleSingleQuoteString && !inTripleDoubleQuoteString &&
							!inSingleLineComment && !inMultiLineComment) {

						// Look ahead to see if this semicolon is at the end of the line
						boolean isEndOfLine = true;
						for (int j = i + 1; j < raw.length(); j++) {
							char next = raw.charAt(j);
							if (next == '\n' || next == '\r') {
								break; // End of line
							} else if (!Character.isWhitespace(next)) {
								isEndOfLine = false;
								break;
							}
							// If it's whitespace but not newline, continue checking
						}

						// Only remove if it's at the end of the line
						if (isEndOfLine) {
							continue; // Skip this semicolon
						}
					}

					result.append(c);
					escaped = false;
				}

				return result.toString();
			};
		}
	}
}
