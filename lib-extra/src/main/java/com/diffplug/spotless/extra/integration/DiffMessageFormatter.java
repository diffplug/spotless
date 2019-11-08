/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.extra.integration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.HistogramDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Splitter;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCell;

/** Formats the messages of failed spotlessCheck invocations with a nice diff message. */
public final class DiffMessageFormatter {
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Builder() {}

		private String runToFix;
		private boolean isPaddedCell;
		private Formatter formatter;
		private List<File> problemFiles;

		/** "Run 'gradlew spotlessApply' to fix these violations." */
		public Builder runToFix(String runToFix) {
			this.runToFix = Objects.requireNonNull(runToFix);
			return this;
		}

		public Builder isPaddedCell(boolean isPaddedCell) {
			this.isPaddedCell = isPaddedCell;
			return this;
		}

		public Builder formatter(Formatter formatter) {
			this.formatter = Objects.requireNonNull(formatter);
			return this;
		}

		public Builder problemFiles(List<File> problemFiles) {
			this.problemFiles = Objects.requireNonNull(problemFiles);
			Preconditions.checkArgument(!problemFiles.isEmpty(), "cannot be empty");
			return this;
		}

		/** Returns the error message. */
		public String getMessage() {
			try {
				Objects.requireNonNull(runToFix, "runToFix");
				Objects.requireNonNull(formatter, "formatter");
				Objects.requireNonNull(problemFiles, "problemFiles");
				DiffMessageFormatter diffFormater = new DiffMessageFormatter(this);
				return "The following files had format violations:\n"
						+ diffFormater.buffer
						+ runToFix;
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		}

		String relativePath(File file) {
			return formatter.getRootDir().relativize(file.toPath()).toString();
		}
	}

	private static final int MAX_CHECK_MESSAGE_LINES = 50;
	public static final int MAX_FILES_TO_LIST = 10;

	private final StringBuilder buffer = new StringBuilder(MAX_CHECK_MESSAGE_LINES * 64);
	private int numLines = 0;

	private DiffMessageFormatter(Builder builder) throws IOException {
		ListIterator<File> problemIter = builder.problemFiles.listIterator();
		while (problemIter.hasNext() && numLines < MAX_CHECK_MESSAGE_LINES) {
			File file = problemIter.next();
			addFile(builder.relativePath(file) + "\n" + DiffMessageFormatter.diff(builder, file));
		}
		if (problemIter.hasNext()) {
			int remainingFiles = builder.problemFiles.size() - problemIter.nextIndex();
			if (remainingFiles >= MAX_FILES_TO_LIST) {
				buffer.append("Violations also present in ").append(remainingFiles).append(" other files.\n");
			} else {
				buffer.append("Violations also present in:\n");
				while (problemIter.hasNext()) {
					addIntendedLine(NORMAL_INDENT, builder.relativePath(problemIter.next()));
				}
			}
		}
	}

	private static final int MIN_LINES_PER_FILE = 4;
	private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');

	private void addFile(String arg) {
		// at the very least, we'll print this about a file:
		//     0.txt
		//         @@ -1,2 +1,2 @@,
		//         -1\\r\\n,
		//         -2\\r\\n,
		//     ... (more lines that didn't fit)
		List<String> lines = NEWLINE_SPLITTER.splitToList(arg);
		if (!lines.isEmpty()) {
			addIntendedLine(NORMAL_INDENT, lines.get(0));
		}
		for (int i = 1; i < Math.min(MIN_LINES_PER_FILE, lines.size()); ++i) {
			addIntendedLine(DIFF_INDENT, lines.get(i));
		}

		// then we'll print the rest that can fit
		ListIterator<String> iter = lines.listIterator(Math.min(MIN_LINES_PER_FILE, lines.size()));
		while (iter.hasNext() && numLines < MAX_CHECK_MESSAGE_LINES) {
			addIntendedLine(DIFF_INDENT, iter.next());
		}

		if (numLines >= MAX_CHECK_MESSAGE_LINES) {
			// we're out of space
			if (iter.hasNext()) {
				int linesLeft = lines.size() - iter.nextIndex();
				addIntendedLine(NORMAL_INDENT, "... (" + linesLeft + " more lines that didn't fit)");
			}
		}
	}

	private static final String NORMAL_INDENT = "    ";
	private static final String DIFF_INDENT = NORMAL_INDENT + NORMAL_INDENT;

	private void addIntendedLine(String indent, String line) {
		buffer.append(indent);
		buffer.append(line);
		buffer.append('\n');
		++numLines;
	}

	/**
	 * Returns a git-style diff between the contents of the given file and what those contents would
	 * look like if formatted using the given formatter. Does not end with any newline
	 * sequence (\n, \r, \r\n).
	 */
	private static String diff(Builder builder, File file) throws IOException {
		Charset encoding = builder.formatter.getEncoding();
		String raw = new String(Files.readAllBytes(file.toPath()), encoding);
		String rawUnix = LineEnding.toUnix(raw);
		String formattedUnix;
		if (builder.isPaddedCell) {
			formattedUnix = PaddedCell.check(builder.formatter, file, rawUnix).canonical();
		} else {
			formattedUnix = builder.formatter.compute(rawUnix, file);
		}
		String formatted = builder.formatter.computeLineEndings(formattedUnix, file);
		return visualizeDiff(raw, formatted);
	}

	private static String visualizeDiff(String raw, String formattedBytes) throws IOException {
		RawText a = new RawText(raw.getBytes(StandardCharsets.UTF_8));
		RawText b = new RawText(formattedBytes.getBytes(StandardCharsets.UTF_8));
		EditList edits = new HistogramDiff().diff(RawTextComparator.DEFAULT, a, b);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		// defaultCharset is here so the formatter could select "fancy" or "simple"
		// characters for whitespace visualization based on the capabilities of the console
		// For instance, if the app is running with file.encoding=ISO-8859-1, then
		// the console can't encode fancy whitespace characters, and the formatter would
		// resort to simple "\\r", "\\n", and so on
		new WriteSpaceAwareDiffFormatter(out, Charset.defaultCharset()).format(edits, a, b);
		return new String(out.toByteArray(), StandardCharsets.UTF_8);
	}
}
