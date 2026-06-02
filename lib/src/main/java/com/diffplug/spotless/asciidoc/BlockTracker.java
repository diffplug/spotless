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

import edu.umd.cs.findbugs.annotations.Nullable;

class BlockTracker {
	private char delimChar = '\0';

	boolean isOpen() {
		return delimChar != '\0';
	}

	void open(CharSequence line) {
		delimChar = line.charAt(0);
	}

	@Nullable String tryClose(CharSequence line) {
		if (delimChar != '\0' && line.length() >= 4 && isAllSameChar(line, delimChar)) {
			String closed = String.valueOf(delimChar);
			delimChar = '\0';
			return closed;
		}
		return null;
	}

	private static boolean isAllSameChar(CharSequence line, char c) {
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) != c) {
				return false;
			}
		}
		return true;
	}
}
