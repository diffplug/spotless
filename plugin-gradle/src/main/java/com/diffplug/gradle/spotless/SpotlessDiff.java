package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

/**
 * SpotlessDiff generates diffs between original and formatted code
 * and converts them to a format compatible with ReviewDog.
 */
public class SpotlessDiff {
	private final Formatter formatter;
	private final Path rootDir;

	private static final Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
	private static final Pattern LINE_PATTERN = Pattern.compile("^([+-])(.*)$", Pattern.MULTILINE);

	public SpotlessDiff(Formatter formatter) {
		this.formatter = formatter;
		this.rootDir = Paths.get(".");
	}

	/**
	 * Generates a ReviewDog-compatible diff for a file
	 */
	public String generateDiff(File file) throws IOException {
		Map.Entry<Integer, String> diffResult =
			DiffMessageFormatter.diff(rootDir, formatter, file);

		int firstDiffLine = diffResult.getKey();
		String diffContent = diffResult.getValue();

		return convertToReviewDogFormat(file.getPath(), firstDiffLine, diffContent);
	}

	/**
	 * Generates diffs for multiple files
	 */
	public String generateDiffs(List<File> files) throws IOException {
		List<String> diagnostics = new ArrayList<>();
		for (File file : files) {
			String diff = generateDiff(file);
			if (diff.startsWith("[") && diff.endsWith("]")) {
				diff = diff.substring(1, diff.length() - 1);
				if (!diff.isEmpty()) {
					diagnostics.add(diff);
				}
			}
		}
		return "[" + String.join(",", diagnostics) + "]";
	}

	/**
	 * Converts the diff content to ReviewDog format
	 */
	private String convertToReviewDogFormat(String filePath, int firstDiffLine, String diffContent) {
		List<String> diagnostics = new ArrayList<>();

		Matcher hunkMatcher = HUNK_PATTERN.matcher(diffContent);

		while (hunkMatcher.find()) {
			int beginLine = Integer.parseInt(hunkMatcher.group(1));
			int lineCount = Integer.parseInt(hunkMatcher.group(2));
			int endLine = beginLine + lineCount - 1;

			StringBuilder suggestionText = new StringBuilder();
			Matcher lineMatcher = LINE_PATTERN.matcher(diffContent);

			while (lineMatcher.find()) {
				char type = lineMatcher.group(1).charAt(0);
				String lineContent = lineMatcher.group(2);

				if (type == '+') {
					if (suggestionText.length() > 0) {
						suggestionText.append("\n");
					}
					suggestionText.append(lineContent);
				}
			}

			String escapedSuggestion = escapeJson(suggestionText.toString());

			String diagnostic = String.format(
				"{\"message\":\"Code formatting suggestion\",\"location\":{\"path\":\"%s\",\"range\":{\"start\":{\"line\":%d,\"column\":1},\"end\":{\"line\":%d,\"column\":1}}},\"severity\":\"WARNING\",\"source\":{\"name\":\"spotless\"},\"suggestions\":[{\"range\":{\"start\":{\"line\":%d,\"column\":1},\"end\":{\"line\":%d,\"column\":1}},\"text\":\"%s\"}]}",
				filePath, beginLine, endLine, beginLine, endLine, escapedSuggestion
			);

			diagnostics.add(diagnostic);
		}

		return "[" + String.join(",", diagnostics) + "]";
	}

	private String escapeJson(String str) {
		return str.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\b", "\\b")
			.replace("\f", "\\f")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}
}
