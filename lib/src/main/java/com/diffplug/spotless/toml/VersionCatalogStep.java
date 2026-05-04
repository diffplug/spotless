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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.FormatterStep;

public final class VersionCatalogStep {
	private VersionCatalogStep() {}

	private static final String NAME = "versionCatalog";

	private static final List<String> TABLE_ORDER = Arrays.asList(
			"[versions]",
			"[libraries]",
			"[bundles]",
			"[plugins]");

	private static final Pattern TABLE_HEADER = Pattern.compile("^\\[([a-zA-Z0-9_-]+)]\\s*$");
	private static final Pattern ENTRY_LINE = Pattern.compile("^([^=]+)=(.+)$");

	public static FormatterStep create() {
		return FormatterStep.create(NAME,
				VersionCatalogStep.class,
				unused -> VersionCatalogStep::format);
	}

	static String format(String raw) {
		if (raw.trim().isEmpty()) {
			return raw;
		}

		Map<String, List<String>> sections = parseSections(raw);
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
			List<String> entries = sections.get(header);
			if (!first) {
				result.append('\n');
			}
			first = false;
			result.append(header).append('\n');
			List<String> formatted = new ArrayList<>();
			for (String entry : entries) {
				formatted.add(formatEntry(entry));
			}
			Collections.sort(formatted);
			for (String entry : formatted) {
				result.append(entry).append('\n');
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
			String trimmed = line.trim();
			if (!trimmed.isEmpty()) {
				preamble.add(trimmed);
			}
		}
		return preamble;
	}

	private static Map<String, List<String>> parseSections(String raw) {
		Map<String, List<String>> sections = new LinkedHashMap<>();
		String currentHeader = null;
		List<String> currentEntries = null;

		for (String line : raw.split("\n", -1)) {
			String trimmed = line.trim();
			if (trimmed.isEmpty() || (currentHeader == null && !TABLE_HEADER.matcher(trimmed).matches())) {
				continue;
			}
			Matcher headerMatcher = TABLE_HEADER.matcher(trimmed);
			if (headerMatcher.matches()) {
				currentHeader = "[" + headerMatcher.group(1) + "]";
				currentEntries = new ArrayList<>();
				sections.put(currentHeader, currentEntries);
			} else if (currentEntries != null && !trimmed.startsWith("#")) {
				currentEntries.add(trimmed);
			}
		}

		return sections;
	}

	static String formatEntry(String entry) {
		Matcher matcher = ENTRY_LINE.matcher(entry);
		if (!matcher.matches()) {
			return entry;
		}

		String key = matcher.group(1).trim();
		String value = matcher.group(2).trim();

		value = formatValue(value);

		return key + " = " + value;
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
		for (int i = 0; i < pairs.length; i++) {
			if (i > 0) {
				result.append(", ");
			}
			String pair = pairs[i].trim();
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
}
