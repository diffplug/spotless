/*
 * Copyright 2015 DiffPlug
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
package com.diffplug.gradle.spotless;

/** Simple step which checks for consistent indentation characters. */
public class IndentStep {
	public enum Type {
		TAB, SPACE
	}

	private final Type type;
	private final int tabsToSpaces;

	public IndentStep(Type type, int tabsToSpaces) {
		this.type = type;
		this.tabsToSpaces = tabsToSpaces;
	}

	private final StringBuilder builder = new StringBuilder();

	public String format(String raw) {
		// reset the buffer
		builder.setLength(0);
		int lineStart = 0; // beginning of line
		do {
			int contentStart = lineStart; // beginning of non-whitespace
			int numSpaces = 0;
			char c;
			while (contentStart < raw.length() && isSpaceOrTab(c = raw.charAt(contentStart))) {
				switch (c) {
				case ' ':
					++numSpaces;
					break;
				case '\t':
					numSpaces += tabsToSpaces;
					break;
				default:
					throw new IllegalArgumentException("Unexpected char " + c);
				}
				++contentStart;
			}

			// add the leading space in a canonical way
			if (numSpaces > 0) {
				switch (type) {
				case SPACE:
					for (int i = 0; i < numSpaces; ++i) {
						builder.append(' ');
					}
					break;
				case TAB:
					for (int i = 0; i < numSpaces / tabsToSpaces; ++i) {
						builder.append('\t');
					}
					break;
				default:
					throw new IllegalArgumentException("Unexpected enum " + type);
				}
			}

			// find the start of the next line
			lineStart = raw.indexOf('\n', contentStart);
			if (lineStart == -1) {
				// if we're at the end, append all of it
				builder.append(raw.subSequence(contentStart, raw.length()));
				return builder.toString();
			} else {
				// increment lineStart by 1 so that we start after the newline next time 
				++lineStart;
				builder.append(raw.subSequence(contentStart, lineStart));
			}
		} while (true);
	}

	private static boolean isSpaceOrTab(char c) {
		return c == ' ' || c == '\t';
	}
}
