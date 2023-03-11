/*
 * Copyright 2016-2023 DiffPlug
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

import com.diffplug.spotless.FormatterStep;

public final class EndWithNewlineStep {
	// prevent direct instantiation
	private EndWithNewlineStep() {}

	/** Creates a FormatterStep which forces lines to end with a newline. */
	public static FormatterStep create() {
		return FormatterStep.create("endWithNewline",
				EndWithNewlineStep.class,
				unused -> EndWithNewlineStep::format);
	}

	private static String format(String rawUnix) {
		// simplifies the logic below if we can assume length > 0
		if (rawUnix.isEmpty()) {
			return "\n";
		}

		// find the last character which has real content
		int lastContentCharacter = rawUnix.length() - 1;
		char c;
		while (lastContentCharacter >= 0) {
			c = rawUnix.charAt(lastContentCharacter);
			if (c == '\n' || c == '\t' || c == ' ') {
				--lastContentCharacter;
			} else {
				break;
			}
		}

		// if it's already clean, no need to create another string
		if (lastContentCharacter == -1) {
			return "\n";
		} else if (lastContentCharacter == rawUnix.length() - 2 && rawUnix.charAt(rawUnix.length() - 1) == '\n') {
			return rawUnix;
		} else {
			return rawUnix.substring(0, lastContentCharacter + 1) + '\n';
		}
	}
}
