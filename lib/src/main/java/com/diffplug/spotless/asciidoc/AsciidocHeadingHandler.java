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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Handles transformations for Asciidoc headings. */
final class AsciidocHeadingHandler {
	private AsciidocHeadingHandler() {}

	// Heading with trailing = signs: == Title == or === Title ===
	// Captured groups: (1) leading equals, (2) title text (trimmed)
	private static final Pattern SYMMETRIC_HEADING = Pattern.compile("^(={1,6})\\s+(.*\\S)\\s+=+\\s*$");

	// Section heading: = Title or == Title, etc.
	// Captured groups: (1) leading equals, (2) trimmed title text
	static final Pattern SECTION_HEADING = Pattern.compile("^(={1,6})\\s+(\\S.*?)\\s*$");

	// ATX heading prefixes for setext -> ATX conversion: ATX_PREFIX[n] = "=".repeat(n+1) + " "
	private static final String[] ATX_PREFIX = {"= ", "== ", "=== ", "==== ", "===== ", "====== "};

	static void normalizeSetextHeadings(List<String> lines) {
		BlockTracker bt = new BlockTracker();
		int readIdx = 0;
		int writeIdx = 0;
		while (readIdx < lines.size()) {
			String line = lines.get(readIdx);
			if (bt.isOpen()) {
				lines.set(writeIdx++, line);
				bt.tryClose(line);
				readIdx++;
				continue;
			}
			if (AsciidocSupport.isBlockDelimiter(line)) {
				lines.set(writeIdx++, line);
				bt.open(line);
				readIdx++;
				continue;
			}
			if (readIdx + 1 < lines.size()) {
				Integer level = AsciidocSupport.detectSetextUnderline(line, lines.get(readIdx + 1));
				if (level != null) {
					lines.set(writeIdx++, ATX_PREFIX[level] + line);
					readIdx += 2;
					continue;
				}
			}
			lines.set(writeIdx++, line);
			readIdx++;
		}
		if (writeIdx < lines.size()) {
			lines.subList(writeIdx, lines.size()).clear();
		}
	}

	static void removeTrailingHeaderEqualsSign(List<String> lines) {
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			Matcher symmetric = SYMMETRIC_HEADING.matcher(line);
			if (symmetric.matches()) {
				lines.set(i, symmetric.group(1) + ' ' + symmetric.group(2));
				continue;
			}
			Matcher section = SECTION_HEADING.matcher(line);
			if (section.matches()) {
				lines.set(i, section.group(1) + ' ' + section.group(2));
			}
		}
	}

	static void ensureHeadingBlankLines(List<String> lines) {
		List<String> result = new ArrayList<>(lines.size() + 8);
		BlockTracker bt = new BlockTracker();

		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);

			if (bt.isOpen()) {
				result.add(line);
				bt.tryClose(line);
				continue;
			}
			if (AsciidocSupport.isBlockDelimiter(line)) {
				result.add(line);
				bt.open(line);
				continue;
			}

			if (SECTION_HEADING.matcher(line).matches()) {
				if (!result.isEmpty() && !result.get(result.size() - 1).isBlank()) {
					result.add("");
				}
				result.add(line);
				if (i + 1 < lines.size() && !lines.get(i + 1).isBlank()) {
					result.add("");
				}
			} else {
				result.add(line);
			}
		}
		lines.clear();
		lines.addAll(result);
	}
}
