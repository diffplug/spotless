/*
 * Copyright 2016-2025 DiffPlug
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

import static com.diffplug.spotless.generic.IndentStep.Type.SPACE;
import static com.diffplug.spotless.generic.IndentStep.Type.TAB;

import java.io.Serial;
import java.io.Serializable;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializedFunction;

/** Simple step which checks for consistent indentation characters. */
public final class IndentStep implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;

	final Type type;
	final int numSpacesPerTab;

	private IndentStep(Type type, int numSpacesPerTab) {
		this.type = type;
		this.numSpacesPerTab = numSpacesPerTab;
	}

	private static final int DEFAULT_NUM_SPACES_PER_TAB = 4;

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
		return FormatterStep.create("indentWith" + type.tabSpace("Tabs", "Spaces"),
				new IndentStep(type, numSpacesPerTab), SerializedFunction.identity(),
				IndentStep::startFormatting);
	}

	private FormatterFunc startFormatting() {
		var runtime = new Runtime(this);
		return runtime::format;
	}

	static class Runtime {
		final IndentStep state;
		final StringBuilder builder = new StringBuilder();

		Runtime(IndentStep state) {
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

				// detect potential multi-line comments
				boolean mightBeMultiLineComment = (contentStart < raw.length()) && (raw.charAt(contentStart) == '*');

				// add the leading space in a canonical way
				if (numSpaces > 0) {
					if (state.type == SPACE) {
						for (int i = 0; i < numSpaces; i++) {
							builder.append(' ');
						}
					} else if (state.type == TAB) {
						for (int i = 0; i < numSpaces / state.numSpacesPerTab; i++) {
							builder.append('\t');
						}
						if (mightBeMultiLineComment && (numSpaces % state.numSpacesPerTab == 1)) {
							builder.append(' ');
						}
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
