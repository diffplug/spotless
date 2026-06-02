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
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/** Handles transformations for Asciidoc blocks (delimiters, source blocks). */
final class AsciidocBlockHandler {
	private final List<String> lines;

	AsciidocBlockHandler(List<String> lines) {
		this.lines = lines;
	}

	private static final String BLOCK_DELIMITER_CHARS = "-=.*_+/";

	// Source / listing block attribute lines: [source], [source,java], [listing], [source%linenums,java], [source#id,java], etc.
	private static final Pattern SOURCE_BLOCK_ATTR = Pattern.compile("^\\[(source|listing)[,\\]%#].*");

	static boolean isBlockDelimiter(CharSequence line) {
		int len = line.length();
		if (len < 4) {
			return false;
		}
		char c = line.charAt(0);
		return BLOCK_DELIMITER_CHARS.indexOf(c) >= 0 && IntStream.range(1, len).noneMatch(i -> line.charAt(i) != c);
	}

	void normalizeBlockDelimiters() {
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (bt.isOpen()) {
				String closed = bt.tryClose(line);
				if (closed != null) {
					lines.set(i, closed.repeat(4));
				}
			} else if (isOverLongBlockDelimiter(line)) {
				String prev = i == 0 ? null : lines.get(i - 1);
				boolean notSetextUnderline = prev == null || prev.isBlank()
						|| AsciidocHeadingHandler.detectSetextUnderline(prev, line) == null;
				if (notSetextUnderline) {
					lines.set(i, String.valueOf(line.charAt(0)).repeat(4));
					bt.open(line);
				}
			} else if (isBlockDelimiter(line)) {
				bt.open(line);
			}
		}
	}

	private static boolean isOverLongBlockDelimiter(CharSequence line) {
		return line.length() > 4 && isBlockDelimiter(line);
	}

	void ensureSourceDelimiters() {
		Collection<String> result = new ArrayList<>(lines.size() + 8);
		BlockTracker bt = new BlockTracker();
		int i = 0;
		while (i < lines.size()) {
			String line = lines.get(i);

			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				i++;
				continue;
			}

			if (isBlockDelimiter(line)) {
				result.add(line);
				bt.open(line);
				i++;
				continue;
			}

			if (SOURCE_BLOCK_ATTR.matcher(line).matches()) {
				result.add(line);
				i++;
				if (i < lines.size()) {
					String next = lines.get(i);
					if (isBlockDelimiter(next)) {
						result.add(next);
						bt.open(next);
						i++;
					} else if (!next.isBlank() && !next.startsWith("[")) {
						result.add("----");
						while (i < lines.size() && !lines.get(i).isBlank()) {
							result.add(lines.get(i));
							i++;
						}
						result.add("----");
					}
				}
				continue;
			}

			result.add(line);
			i++;
		}
		lines.clear();
		lines.addAll(result);
	}
}
