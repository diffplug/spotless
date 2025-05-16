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
package com.diffplug.spotless.extra.middleware;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Lint;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

/**
 * ReviewDogGenerator generates ReviewDog formatted output for linting issues
 * based on Spotless formatting results.
 */
public final class ReviewDogGenerator {
	private final Formatter formatter;
	private final File projectDir;

	/**
	 * Constructor for ReviewDogGenerator.
	 *
	 * @param projectDir the root directory of the project
	 * @param steps      the list of FormatterStep to apply
	 */
	public ReviewDogGenerator(File projectDir, List<FormatterStep> steps) {
		this.projectDir = projectDir;
		this.formatter = Formatter.builder()
				.encoding(StandardCharsets.UTF_8)
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.steps(steps)
				.build();
	}

	/**
	 * Generates ReviewDog formatted output for the given list of files.
	 *
	 * @param files the list of files to check
	 * @return a String containing ReviewDog formatted lint messages with code suggestions
	 * @throws IOException if file reading fails
	 */
	public String generateReviewDogFormat(List<File> files) throws IOException {
		StringBuilder reviewDogOutput = new StringBuilder();

		for (File file : files) {
			LintState lintState = LintState.of(formatter, file);

			if (!lintState.getDirtyState().isClean()) {
				String relativePath = getRelativePath(file);

				Map.Entry<Integer, String> diffResult = DiffMessageFormatter.diff(
						projectDir.toPath(), formatter, file);

				List<DiffHunk> hunks = parseDiffHunks(diffResult.getValue());
				List<ReviewDogIssue> issues = processLints(lintState, hunks);

				for (ReviewDogIssue issue : issues) {
					reviewDogOutput.append(formatReviewDogLine(relativePath, issue));
				}
			}
		}

		return reviewDogOutput.toString();
	}

	/**
	 * Converts an absolute file path to a relative path based on the project directory.
	 *
	 * @param file the file to convert
	 * @return relative path string
	 */
	private String getRelativePath(File file) {
		return projectDir.toURI().relativize(file.toURI()).getPath();
	}

	/**
	 * Parses git-style diff content into structured diff hunks.
	 *
	 * @param diffContent the git-style diff content
	 * @return list of parsed DiffHunk objects
	 */
	private List<DiffHunk> parseDiffHunks(String diffContent) {
		List<DiffHunk> hunks = new ArrayList<>();
		if (diffContent == null || diffContent.isEmpty()) {
			return hunks;
		}

		Pattern hunkHeaderPattern = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
		String[] lines = diffContent.split("\n");

		DiffHunk currentHunk = null;
		StringBuilder hunkContent = new StringBuilder();

		for (String line : lines) {
			// Skip file header lines (--- and +++)
			if (line.startsWith("---") || line.startsWith("+++")) {
				continue;
			}

			if (line.startsWith("@@")) {
				if (currentHunk != null) {
					currentHunk.content = hunkContent.toString().trim();
					hunks.add(currentHunk);
					hunkContent = new StringBuilder();
				}

				Matcher matcher = hunkHeaderPattern.matcher(line);
				if (matcher.find()) {
					int originalStart = Integer.parseInt(matcher.group(1));
					int originalLength = Integer.parseInt(matcher.group(2));
					int newStart = Integer.parseInt(matcher.group(3));
					int newLength = Integer.parseInt(matcher.group(4));

					currentHunk = new DiffHunk(originalStart, originalLength, newStart, newLength);
					currentHunk.header = line;
				}
				hunkContent.append(line).append("\n");
			} else if (currentHunk != null) {
				hunkContent.append(line).append("\n");
			}
		}

		if (currentHunk != null) {
			currentHunk.content = hunkContent.toString().trim();
			hunks.add(currentHunk);
		}

		return hunks;
	}

	/**
	 * Processes lint information and associates them with appropriate diff hunks.
	 *
	 * @param lintState the LintState containing all lint information
	 * @param hunks list of diff hunks
	 * @return list of ReviewDogIssue
	 */
	private List<ReviewDogIssue> processLints(LintState lintState, List<DiffHunk> hunks) {
		List<ReviewDogIssue> issues = new ArrayList<>();

		List<List<Lint>> lintsPerStep = lintState.getLintsPerStep();

		for (List<Lint> stepLints : Objects.requireNonNull(lintsPerStep)) {
			for (Lint lint : stepLints) {
				DiffHunk relevantHunk = findRelevantHunk(hunks, lint.getLineStart());

				String suggestion = "";
				if (relevantHunk != null) {
					suggestion = extractSuggestionFromHunk(relevantHunk);
				}

				ReviewDogIssue issue = new ReviewDogIssue(
						lint.getLineStart(),
						1,
						lint.getShortCode() + ": " + lint.getDetail(),
						suggestion);

				issues.add(issue);
			}
		}

		// If no specific lints were found but file is dirty, add a general formatting issue
		if (issues.isEmpty() && !hunks.isEmpty()) {
			DiffHunk firstHunk = hunks.get(0);
			String suggestion = extractSuggestionFromHunk(firstHunk);

			issues.add(new ReviewDogIssue(
					firstHunk.originalStart,
					1,
					"General formatting issue: file needs to be reformatted.",
					suggestion));
		}

		return issues;
	}

	/**
	 * Finds the hunk that is relevant for a specific line number.
	 *
	 * @param hunks list of diff hunks
	 * @param lineNumber the line number to find (1-based)
	 * @return the relevant hunk or null if not found
	 */
	private DiffHunk findRelevantHunk(List<DiffHunk> hunks, int lineNumber) {
		for (DiffHunk hunk : hunks) {
			if (lineNumber >= hunk.originalStart &&
					lineNumber < hunk.originalStart + hunk.originalLength) {
				return hunk;
			}
		}

		// If no exact match is found, find the closest hunk before the line number
		DiffHunk closestHunk = null;
		int closestDistance = Integer.MAX_VALUE;

		for (DiffHunk hunk : hunks) {
			if (hunk.originalStart <= lineNumber) {
				int distance = lineNumber - hunk.originalStart;
				if (distance < closestDistance) {
					closestDistance = distance;
					closestHunk = hunk;
				}
			}
		}
		return closestHunk;
	}

	/**
	 * Extracts suggestion content from a diff hunk.
	 *
	 * @param hunk the diff hunk
	 * @return the suggestion content
	 */
	private String extractSuggestionFromHunk(DiffHunk hunk) {
		StringBuilder suggestion = new StringBuilder();

		String[] lines = hunk.content.split("\n");
		boolean headerProcessed = false;

		for (String line : lines) {
			if (!headerProcessed && line.startsWith("@@")) {
				headerProcessed = true;
				continue;
			}

			if (headerProcessed) {
				if (line.startsWith("+") && !line.startsWith("+++")) {
					suggestion.append(line.substring(1));
					suggestion.append("\n");
				} else if (!line.startsWith("-") && !line.startsWith("---")) {
					suggestion.append(line);
					suggestion.append("\n");
				}
			}
		}

		return suggestion.toString().trim();
	}

	/**
	 * Formats a single ReviewDog issue line according to ReviewDog's expected format
	 * with suggestion support.
	 *
	 * @param filePath the relative file path
	 * @param issue    the ReviewDogIssue object
	 * @return formatted string line with suggestion
	 */
	private String formatReviewDogLine(String filePath, ReviewDogIssue issue) {
		StringBuilder builder = new StringBuilder();

		builder.append(String.format("%s:%d:%d: %s\n",
				filePath, issue.lineNumber, issue.column, issue.message));

		if (issue.suggestion != null && !issue.suggestion.isEmpty()) {
			builder.append("```suggestion\n");
			builder.append(issue.suggestion);
			builder.append("\n```\n");
		}

		return builder.toString();
	}

	/**
	 * Inner class representing a diff hunk.
	 */
	private static class DiffHunk {
		final int originalStart;
		final int originalLength;
		final int newStart;
		final int newLength;
		String header;
		String content;

		DiffHunk(int originalStart, int originalLength, int newStart, int newLength) {
			this.originalStart = originalStart;
			this.originalLength = originalLength;
			this.newStart = newStart;
			this.newLength = newLength;
		}
	}

	/**
	 * Inner class representing a ReviewDog issue.
	 */
	private static class ReviewDogIssue {
		final int lineNumber;
		final int column;
		final String message;
		final String suggestion;

		ReviewDogIssue(int lineNumber, int column, String message, String suggestion) {
			this.lineNumber = lineNumber;
			this.column = column;
			this.message = message;
			this.suggestion = suggestion;
		}
	}
}
