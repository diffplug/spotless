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
package com.diffplug.spotless.toml;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;

public final class VersionCatalogStep {
	private VersionCatalogStep() {}

	private static final String NAME = "versionCatalog";
	private static final int DEFAULT_MAX_LINE_LENGTH = 120;

	private static final List<String> TABLE_ORDER = Arrays.asList(
			"[versions]",
			"[libraries]",
			"[bundles]",
			"[plugins]");

	private static final Pattern TABLE_HEADER = Pattern.compile("^\\[([a-zA-Z0-9_-]+)]\\s*$");
	private static final Pattern ENTRY_LINE = Pattern.compile("^([^=]+)=(.+)$");

	public static FormatterStep create() {
		return create(false, DEFAULT_MAX_LINE_LENGTH);
	}

	public static FormatterStep create(boolean stripQuotedKeys) {
		return create(stripQuotedKeys, DEFAULT_MAX_LINE_LENGTH);
	}

	public static FormatterStep create(boolean stripQuotedKeys, int maxLineLength) {
		return FormatterStep.createLazy(NAME,
				() -> new State(stripQuotedKeys, maxLineLength),
				State::toFormatter);
	}

	static String format(String raw, boolean stripQuotedKeys, int maxLineLength) {
		if (raw.trim().isEmpty()) {
			return raw;
		}

		Map<String, List<Entry>> sections = parseSections(raw);
		List<String> preambleLines = extractPreamble(raw);

		StringBuilder result = new StringBuilder();

		for (String line : preambleLines) {
			result.append(line).append('\n');
		}

		boolean first = preambleLines.isEmpty();

		List<String> orderedKeys = new ArrayList<>();
		for (String key : TABLE_ORDER) {
			if (sections.containsKey(key)) {
				orderedKeys.add(key);
			}
		}
		for (String key : sections.keySet()) {
			if (!TABLE_ORDER.contains(key)) {
				orderedKeys.add(key);
			}
		}

		for (String header : orderedKeys) {
			List<Entry> entries = sections.get(header);
			if (!first) {
				result.append('\n');
			}
			first = false;
			result.append(header).append('\n');

			for (Entry entry : entries) {
				String formatted = formatEntry(entry.content, stripQuotedKeys);
				entry.formatted = applyLineLength(formatted, maxLineLength);
			}
			Collections.sort(entries, Comparator.comparing(Entry::sortKey));

			for (Entry entry : entries) {
				for (String commentLine : entry.leadingComments) {
					result.append(commentLine).append('\n');
				}
				result.append(entry.formatted).append('\n');
			}
		}

		return result.toString();
	}

	private static List<String> extractPreamble(String raw) {
		List<String> preamble = new ArrayList<>();
		for (String line : raw.split("\n", -1)) {
			if (TABLE_HEADER.matcher(line.trim()).matches()) {
				break;
			}
			preamble.add(line);
		}
		while (!preamble.isEmpty() && preamble.get(preamble.size() - 1).trim().isEmpty()) {
			preamble.remove(preamble.size() - 1);
		}
		return preamble;
	}

	private static Map<String, List<Entry>> parseSections(String raw) {
		Map<String, List<Entry>> sections = new LinkedHashMap<>();
		String currentHeader = null;
		List<Entry> currentEntries = null;
		List<String> pendingComments = new ArrayList<>();
		StringBuilder multiLineAccumulator = null;

		for (String line : raw.split("\n", -1)) {
			String trimmed = line.trim();

			if (multiLineAccumulator != null) {
				multiLineAccumulator.append(' ').append(trimmed);
				if (isBalanced(multiLineAccumulator.toString())) {
					Entry entry = new Entry(multiLineAccumulator.toString(), new ArrayList<>(pendingComments));
					currentEntries.add(entry);
					pendingComments.clear();
					multiLineAccumulator = null;
				}
				continue;
			}

			if (currentHeader == null && !TABLE_HEADER.matcher(trimmed).matches()) {
				continue;
			}
			Matcher headerMatcher = TABLE_HEADER.matcher(trimmed);
			if (headerMatcher.matches()) {
				currentHeader = "[" + headerMatcher.group(1) + "]";
				currentEntries = new ArrayList<>();
				sections.put(currentHeader, currentEntries);
				pendingComments.clear();
			} else if (currentEntries != null) {
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					pendingComments.add(trimmed);
				} else if (!isBalanced(trimmed)) {
					multiLineAccumulator = new StringBuilder(trimmed);
				} else {
					Entry entry = new Entry(trimmed, new ArrayList<>(pendingComments));
					currentEntries.add(entry);
					pendingComments.clear();
				}
			}
		}

		return sections;
	}

	private static boolean isBalanced(String text) {
		int depth = 0;
		boolean inQuote = false;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
				inQuote = !inQuote;
			} else if (!inQuote) {
				if (c == '{' || c == '[') {
					depth++;
				} else if (c == '}' || c == ']') {
					depth--;
				}
			}
		}
		return depth == 0;
	}

	private static String extractKey(String formattedEntry) {
		String firstLine = formattedEntry.contains("\n") ? formattedEntry.substring(0, formattedEntry.indexOf('\n')) : formattedEntry;
		Matcher matcher = ENTRY_LINE.matcher(firstLine);
		if (!matcher.matches()) {
			return formattedEntry;
		}
		String key = matcher.group(1).trim();
		if (key.startsWith("\"") && key.endsWith("\"")) {
			return key.substring(1, key.length() - 1);
		}
		return key;
	}

	static String formatEntry(String entry, boolean stripQuotedKeys) {
		Matcher matcher = ENTRY_LINE.matcher(entry);
		if (!matcher.matches()) {
			return entry;
		}

		String key = matcher.group(1).trim();
		if (stripQuotedKeys && key.startsWith("\"") && key.endsWith("\"")) {
			String bare = key.substring(1, key.length() - 1);
			if (isBareKey(bare)) {
				key = bare;
			}
		}
		String valueAndComment = matcher.group(2).trim();

		String inlineComment = extractInlineComment(valueAndComment);
		String value = inlineComment != null
				? valueAndComment.substring(0, valueAndComment.length() - inlineComment.length()).trim()
				: valueAndComment;

		value = formatValue(value);

		if (inlineComment != null) {
			return key + " = " + value + " " + inlineComment;
		}
		return key + " = " + value;
	}

	private static String applyLineLength(String formattedEntry, int maxLineLength) {
		if (maxLineLength <= 0) {
			return formattedEntry;
		}

		String inlineComment = extractInlineComment(formattedEntry);
		String entryWithoutComment = inlineComment != null
				? formattedEntry.substring(0, formattedEntry.length() - inlineComment.length()).trim()
				: formattedEntry;

		Matcher matcher = ENTRY_LINE.matcher(entryWithoutComment);
		if (!matcher.matches()) {
			return formattedEntry;
		}

		String key = matcher.group(1).trim();
		String value = matcher.group(2).trim();
		String commentSuffix = inlineComment != null ? " " + inlineComment : "";

		if (value.startsWith("{") && value.endsWith("}")) {
			if (formattedEntry.length() > maxLineLength) {
				return splitInlineTable(key, value, commentSuffix);
			}
			return formattedEntry;
		}

		return formattedEntry;
	}

	private static String splitInlineTable(String key, String value, String commentSuffix) {
		String inner = value.substring(1, value.length() - 1).trim();
		String[] pairs = splitTopLevel(inner, ',');

		StringBuilder result = new StringBuilder();
		result.append(key).append(" = {").append(commentSuffix).append('\n');
		for (int i = 0; i < pairs.length; i++) {
			String pair = pairs[i].trim();
			if (pair.isEmpty()) {
				continue;
			}
			result.append("  ").append(pair).append(',').append('\n');
		}
		result.append('}');
		return result.toString();
	}

	private static String extractInlineComment(String valueAndComment) {
		boolean inQuote = false;
		int depth = 0;

		for (int i = 0; i < valueAndComment.length(); i++) {
			char c = valueAndComment.charAt(i);
			if (c == '"' && (i == 0 || valueAndComment.charAt(i - 1) != '\\')) {
				inQuote = !inQuote;
			} else if (!inQuote) {
				if (c == '{' || c == '[') {
					depth++;
				} else if (c == '}' || c == ']') {
					depth--;
				} else if (c == '#' && depth == 0) {
					return valueAndComment.substring(i);
				}
			}
		}
		return null;
	}

	private static String formatValue(String value) {
		if (value.startsWith("{")) {
			return formatInlineTable(value);
		}
		if (value.startsWith("[")) {
			return formatInlineArray(value);
		}
		return value;
	}

	private static String formatInlineTable(String value) {
		if (!value.startsWith("{") || !value.endsWith("}")) {
			return value;
		}

		String inner = value.substring(1, value.length() - 1).trim();
		if (inner.isEmpty()) {
			return "{}";
		}

		String[] pairs = splitTopLevel(inner, ',');
		StringBuilder result = new StringBuilder("{ ");
		boolean first = true;
		for (String rawPair : pairs) {
			String pair = rawPair.trim();
			if (pair.isEmpty()) {
				continue;
			}
			if (!first) {
				result.append(", ");
			}
			first = false;
			Matcher pairMatcher = ENTRY_LINE.matcher(pair);
			if (pairMatcher.matches()) {
				String pairKey = pairMatcher.group(1).trim();
				String pairValue = pairMatcher.group(2).trim();
				pairValue = formatValue(pairValue);
				result.append(pairKey).append(" = ").append(pairValue);
			} else {
				result.append(pair);
			}
		}
		result.append(" }");
		return result.toString();
	}

	private static String formatInlineArray(String value) {
		if (!value.startsWith("[") || !value.endsWith("]")) {
			return value;
		}

		String inner = value.substring(1, value.length() - 1).trim();
		if (inner.isEmpty()) {
			return "[]";
		}

		String[] elements = splitTopLevel(inner, ',');
		StringBuilder result = new StringBuilder("[ ");
		for (int i = 0; i < elements.length; i++) {
			if (i > 0) {
				result.append(", ");
			}
			result.append(elements[i].trim());
		}
		result.append(" ]");
		return result.toString();
	}

	private static String[] splitTopLevel(String input, char delimiter) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		boolean inQuote = false;
		int start = 0;

		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (c == '"' && (i == 0 || input.charAt(i - 1) != '\\')) {
				inQuote = !inQuote;
			} else if (!inQuote) {
				if (c == '{' || c == '[') {
					depth++;
				} else if (c == '}' || c == ']') {
					depth--;
				} else if (c == delimiter && depth == 0) {
					parts.add(input.substring(start, i));
					start = i + 1;
				}
			}
		}
		parts.add(input.substring(start));
		return parts.toArray(new String[0]);
	}

	private static final Pattern BARE_KEY = Pattern.compile("^[a-zA-Z0-9_-]+$");

	private static boolean isBareKey(String key) {
		return BARE_KEY.matcher(key).matches();
	}

	private static final class State implements Serializable {
		private static final long serialVersionUID = 2L;

		private final boolean stripQuotedKeys;
		private final int maxLineLength;

		State(boolean stripQuotedKeys, int maxLineLength) {
			this.stripQuotedKeys = stripQuotedKeys;
			this.maxLineLength = maxLineLength;
		}

		FormatterFunc toFormatter() {
			return raw -> format(raw, stripQuotedKeys, maxLineLength);
		}
	}

	private static final class Entry {
		final String content;
		final List<String> leadingComments;
		String formatted;

		Entry(String content, List<String> leadingComments) {
			this.content = content;
			this.leadingComments = leadingComments;
		}

		String sortKey() {
			return extractKey(formatted != null ? formatted : content);
		}
	}
}
