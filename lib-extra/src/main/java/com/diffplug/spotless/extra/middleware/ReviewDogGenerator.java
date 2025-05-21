/*
 * Copyright 2025 DiffPlug
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
package com.diffplug.spotless.extra.middleware;

import java.util.List;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Lint;

/**
 * Utility class for generating ReviewDog compatible output in the rdjsonl format.
 * This class provides methods to create diff and lint reports that can be used by ReviewDog.
 */
public final class ReviewDogGenerator {

	private ReviewDogGenerator() {
		// Prevent instantiation
	}

	/**
	 * Generates a ReviewDog compatible JSON line (rdjsonl) for a diff between
	 * the actual content and the formatted content of a file.
	 *
	 * @param path The file path
	 * @param actualContent The content as it currently exists in the file
	 * @param formattedContent The content after formatting is applied
	 * @return A string in rdjsonl format representing the diff
	 */
	public static String rdjsonlDiff(String path, String actualContent, String formattedContent) {
		if (actualContent.equals(formattedContent)) {
			return "";
		}

		String diff = createUnifiedDiff(path, actualContent, formattedContent);

		return String.format(
				"{\"message\":{\"path\":\"%s\",\"message\":\"File requires formatting\",\"diff\":\"%s\"}}",
				escapeJson(path),
				escapeJson(diff));
	}

	/**
	 * Generates ReviewDog compatible JSON lines (rdjsonl) for lint issues
	 * identified by formatting steps.
	 *
	 * @param path The file path
	 * @param formattedContent The content after formatting is applied
	 * @param steps The list of formatter steps applied
	 * @param lintsPerStep The list of lints produced by each step
	 * @return A string in rdjsonl format representing the lints
	 */
	public static String rdjsonlLints(String path, String formattedContent,
			List<FormatterStep> steps, List<List<Lint>> lintsPerStep) {
		if (lintsPerStep == null || lintsPerStep.isEmpty()) {
			return "";
		}

		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < lintsPerStep.size(); i++) {
			List<Lint> lints = lintsPerStep.get(i);
			if (lints == null || lints.isEmpty()) {
				continue;
			}

			String stepName = (i < steps.size()) ? steps.get(i).getName() : "unknown";
			for (Lint lint : lints) {
				builder.append(formatLintAsJson(path, lint, stepName)).append('\n');
			}
		}

		return builder.toString().trim();
	}

	/**
	 * Creates a unified diff between two text contents.
	 */
	private static String createUnifiedDiff(String path, String actualContent, String formattedContent) {
		String[] actualLines = actualContent.split("\\r?\\n", -1);
		String[] formattedLines = formattedContent.split("\\r?\\n", -1);

		StringBuilder diff = new StringBuilder();
		diff.append("--- a/").append(path).append('\n');
		diff.append("+++ b/").append(path).append('\n');
		diff.append("@@ -1,").append(actualLines.length).append(" +1,").append(formattedLines.length).append(" @@\n");

		for (String line : actualLines) {
			diff.append('-').append(line).append('\n');
		}

		for (String line : formattedLines) {
			diff.append('+').append(line).append('\n');
		}

		return diff.toString();
	}

	/**
	 * Formats a single lint issue as a JSON line.
	 */
	private static String formatLintAsJson(String path, Lint lint, String stepName) {
		return String.format(
				"{\"message\":{\"path\":\"%s\",\"line\":%d,\"column\":1,\"message\":\"%s: %s\"}}",
				escapeJson(path),
				lint.getLineStart(),
				escapeJson(lint.getShortCode()),
				escapeJson(lint.getDetail()));
	}

	/**
	 * Escapes special characters in a string for JSON compatibility.
	 */
	private static String escapeJson(String str) {
		if (str == null) {
			return "";
		}
		return str
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r")
				.replace("\t", "\\t")
				.replace("\b", "\\b")
				.replace("\f", "\\f");
	}
}
