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
import java.util.ArrayDeque;
import java.util.Deque;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

/**
 * Removes unnecessary semicolons from Groovy code. A semicolon is only removed when it is the last non-whitespace
 * character on its line, outside any string literal or comment, and not part of a multi-line {@code for (init; cond;
 * update)} header.
 * <p>
 * Recognised literals: {@code '...'}, {@code "..."}, {@code '''...'''}, {@code """..."""} and dollar-slashy
 * {@code $/.../$} strings. Plain slashy strings ({@code /.../}) are not recognised, because telling them apart from
 * division requires a full Groovy lexer.
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
			return State::removeSemicolons;
		}

		private enum Mode {
			CODE, LINE_COMMENT, BLOCK_COMMENT, SINGLE_QUOTE, DOUBLE_QUOTE, TRIPLE_SINGLE_QUOTE, TRIPLE_DOUBLE_QUOTE, DOLLAR_SLASHY
		}

		private static String removeSemicolons(String raw) {
			StringBuilder result = new StringBuilder(raw.length());
			Mode mode = Mode.CODE;
			// one entry per open bracket, true if it is the '(' of a `for` header
			Deque<Boolean> brackets = new ArrayDeque<>();
			int i = 0;
			while (i < raw.length()) {
				char c = raw.charAt(i);
				int consumed = 1;
				switch (mode) {
				case CODE:
					if (raw.startsWith("//", i)) {
						mode = Mode.LINE_COMMENT;
						consumed = 2;
					} else if (raw.startsWith("/*", i)) {
						mode = Mode.BLOCK_COMMENT;
						consumed = 2;
					} else if (raw.startsWith("'''", i)) {
						mode = Mode.TRIPLE_SINGLE_QUOTE;
						consumed = 3;
					} else if (raw.startsWith("\"\"\"", i)) {
						mode = Mode.TRIPLE_DOUBLE_QUOTE;
						consumed = 3;
					} else if (raw.startsWith("$/", i) && (i == 0 || !Character.isJavaIdentifierPart(raw.charAt(i - 1)))) {
						mode = Mode.DOLLAR_SLASHY;
						consumed = 2;
					} else if (c == '\'') {
						mode = Mode.SINGLE_QUOTE;
					} else if (c == '"') {
						mode = Mode.DOUBLE_QUOTE;
					} else if (c == '(') {
						brackets.push(isPrecededByFor(raw, i));
					} else if (c == '{' || c == '[') {
						brackets.push(Boolean.FALSE);
					} else if (c == ')' || c == '}' || c == ']') {
						brackets.poll();
					} else if (c == ';' && !Boolean.TRUE.equals(brackets.peek()) && isLastOnLine(raw, i)) {
						i++;
						continue; // drop the semicolon
					}
					break;
				case LINE_COMMENT:
					if (c == '\n' || c == '\r') {
						mode = Mode.CODE;
					}
					break;
				case BLOCK_COMMENT:
					if (raw.startsWith("*/", i)) {
						mode = Mode.CODE;
						consumed = 2;
					}
					break;
				case SINGLE_QUOTE:
				case DOUBLE_QUOTE:
					if (c == '\\') {
						consumed = 2; // skip the escaped character
					} else if (c == (mode == Mode.SINGLE_QUOTE ? '\'' : '"') || c == '\n' || c == '\r') {
						// a newline can't occur in a single-line string, so it means we misread the input: resync
						mode = Mode.CODE;
					}
					break;
				case TRIPLE_SINGLE_QUOTE:
				case TRIPLE_DOUBLE_QUOTE:
					if (c == '\\') {
						consumed = 2; // skip the escaped character, so \''' or \""" doesn't end the string
					} else if (raw.startsWith(mode == Mode.TRIPLE_SINGLE_QUOTE ? "'''" : "\"\"\"", i)) {
						mode = Mode.CODE;
						consumed = 3;
					}
					break;
				case DOLLAR_SLASHY:
					if (raw.startsWith("$$", i) || raw.startsWith("$/", i)) {
						consumed = 2; // escaped dollar or slash
					} else if (raw.startsWith("/$", i)) {
						mode = Mode.CODE;
						consumed = 2;
					}
					break;
				default:
					throw new IllegalStateException("Unknown mode " + mode);
				}
				int end = Math.min(raw.length(), i + consumed);
				result.append(raw, i, end);
				i = end;
			}
			return result.toString();
		}

		private static boolean isPrecededByFor(String raw, int parenIndex) {
			int j = parenIndex - 1;
			while (j >= 0 && Character.isWhitespace(raw.charAt(j))) {
				j--;
			}
			return j >= 2 && raw.startsWith("for", j - 2)
					&& (j < 3 || !Character.isJavaIdentifierPart(raw.charAt(j - 3)));
		}

		private static boolean isLastOnLine(String raw, int semicolonIndex) {
			for (int j = semicolonIndex + 1; j < raw.length(); j++) {
				char next = raw.charAt(j);
				if (next == '\n' || next == '\r') {
					return true;
				} else if (!Character.isWhitespace(next)) {
					return false;
				}
			}
			return true;
		}
	}
}
