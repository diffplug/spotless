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
package com.diffplug.gradle.spotless;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

import com.diffplug.common.base.CharMatcher;

final class DiffUtils {
	private DiffUtils() {}

	private static final CharMatcher NEWLINE_MATCHER = CharMatcher.is('\n');
	private static final char MIDDLE_DOT = '\u00b7';

	/**
	 * Returns a git-style diff between the contents of the given file and what those contents would
	 * look like if formatted using the given formatter. Does not end with any newline
	 * sequence (\n, \r, \r\n).
	 */
	static String diff(File file, Formatter formatter) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		String rawUnix = LineEnding.toUnix(raw);
		String formattedUnix = formatter.applySteps(rawUnix, file);
		if (rawUnix.equals(formattedUnix)) {
			// the formatting is fine, so it's a line-ending issue
			String formatted = formatter.applyLineEndings(formattedUnix, file);
			return diffWhitespaceLineEndings(raw, formatted, false, true);
		} else {
			return diffWhitespaceLineEndings(rawUnix, formattedUnix, true, false);
		}
	}

	/**
	 * Returns a git-style diff between the two unix strings.
	 *
	 * Output has no trailing newlines.
	 *
	 * Boolean args determine whether whitespace or line endings will be visible.
	 */
	private static String diffWhitespaceLineEndings(String dirty, String clean, boolean whitespace, boolean lineEndings) throws IOException {
		dirty = visibleWhitespaceLineEndings(dirty, whitespace, lineEndings);
		clean = visibleWhitespaceLineEndings(clean, whitespace, lineEndings);

		RawText a = new RawText(dirty.getBytes(StandardCharsets.UTF_8));
		RawText b = new RawText(clean.getBytes(StandardCharsets.UTF_8));
		EditList edits = new EditList();
		edits.addAll(MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT, a, b));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DiffFormatter formatter = new DiffFormatter(out)) {
			formatter.format(edits, a, b);
		}
		String formatted = out.toString(StandardCharsets.UTF_8.name());
		return NEWLINE_MATCHER.trimTrailingFrom(formatted);
	}

	/**
	 * Makes the whitespace and/or the lineEndings visible.
	 *
	 * MyersDiff wants inputs with only unix line endings.  So this ensures that that is the case.
	 */
	private static String visibleWhitespaceLineEndings(String input, boolean whitespace, boolean lineEndings) {
		if (whitespace) {
			input = input.replace(' ', MIDDLE_DOT).replace("\t", "\\t");
		}
		if (lineEndings) {
			input = input.replace("\n", "\\n\n").replace("\r", "\\r");
		} else {
			// we want only \n, so if we didn't replace them above, we'll replace them here.
			input = input.replace("\r", "");
		}
		return input;
	}
}
