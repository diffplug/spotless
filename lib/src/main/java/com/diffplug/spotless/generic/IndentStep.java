/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.Serializable;
import java.util.Objects;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

/** Simple step which checks for consistent indentation characters. */
public final class IndentStep {

	private static final int DEFAULT_NUM_SPACES_PER_TAB = 4;

	// prevent direct instantiation
	private IndentStep() {}

	public enum Type {
		TAB, SPACE;

		private <T> T tabSpace(T tab, T space) {
			return this == TAB ? tab : space;
		}

		/** Creates a step which will indent with the given type of whitespace, converting between tabs and spaces at the default ratio. */
		public FormatterStep create() {
			return IndentStep.create(this, defaultNumSpacesPerTab());
		}

		/** Synonym for {@link IndentStep#create(Type, int)}. */
		public FormatterStep create(int numSpacesPerTab) {
			return IndentStep.create(this, numSpacesPerTab);
		}
	}

	/** Creates a step which will indent with the given type of whitespace, converting between tabs and spaces at the given ratio. */
	public static FormatterStep create(Type type, int numSpacesPerTab) {
		Objects.requireNonNull(type, "type");
		return FormatterStep.create("indentWith" + type.tabSpace("Tabs", "Spaces"),
				new State(type, numSpacesPerTab), State::toFormatter);
	}

	private static class State implements Serializable {
		private static final long serialVersionUID = 1L;

		final Type type;
		final int numSpacesPerTab;

		State(Type type, int numSpacesPerTab) {
			this.type = type;
			this.numSpacesPerTab = numSpacesPerTab;
		}

		FormatterFunc toFormatter() {
			return new Runtime(this)::format;
		}
	}

	static class Runtime {
		final State state;
		final StringBuilder builder = new StringBuilder();

		Runtime(State state) {
			this.state = state;
		}

		String format(String raw) {
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
						numSpaces += state.numSpacesPerTab;
						break;
					default:
						throw new IllegalArgumentException("Unexpected char " + c);
					}
					++contentStart;
				}

				// add the leading space in a canonical way
				if (numSpaces > 0) {
					switch (state.type) {
					case SPACE:
						for (int i = 0; i < numSpaces; ++i) {
							builder.append(' ');
						}
						break;
					case TAB:
						for (int i = 0; i < numSpaces / state.numSpacesPerTab; ++i) {
							builder.append('\t');
						}
						break;
					default:
						throw new IllegalArgumentException("Unexpected enum " + state.type);
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
	}

	private static boolean isSpaceOrTab(char c) {
		return c == ' ' || c == '\t';
	}

	public static int defaultNumSpacesPerTab() {
		return DEFAULT_NUM_SPACES_PER_TAB;
	}
}
