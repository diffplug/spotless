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
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.gradle.api.GradleException;

import com.diffplug.common.base.CharMatcher;

final class DiffUtils {
	private DiffUtils() {}

	private static final CharMatcher NEWLINE_MATCHER = CharMatcher.is('\n');

	/**
	 * Returns a git-style diff between the contents of the given file and what those contents would
	 * look like if formatted using the given formatter. Does not end with any newline
	 * sequence (\n, \r, \r\n).
	 */
	static String diff(File file, Formatter formatter) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		String unix = LineEnding.toUnix(raw);
		String formatted = formatter.applyAll(unix, file);
		return DiffUtils.diff(unix, formatted);
	}

	/**
	 * Returns a git-style diff between the two given strings. Does not end with any newline
	 * sequence (\n, \r, \r\n).
	 */
	private static String diff(String first, String second) {
		RawText a = new RawText(first.getBytes(StandardCharsets.UTF_8));
		RawText b = new RawText(second.getBytes(StandardCharsets.UTF_8));
		EditList edits = new EditList();
		edits.addAll(MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT, a, b));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			try (DiffFormatter formatter = new DiffFormatter(out)) {
				formatter.format(edits, a, b);
			}
		} catch (IOException e) {
			throw new GradleException("Unexpected IOException thrown", e);
		}
		try {
			String formatted = out.toString(StandardCharsets.UTF_8.name());
			return NEWLINE_MATCHER.trimTrailingFrom(formatted);
		} catch (UnsupportedEncodingException e) {
			throw new GradleException("StandardCharsets.UTF_8.name() is apparently not a supported encoding name", e);
		}
	}
}
