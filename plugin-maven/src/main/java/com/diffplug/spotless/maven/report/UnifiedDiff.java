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
package com.diffplug.spotless.maven.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

/**
 * A git-style unified diff with three lines of context, computed with the same JGit diff that
 * {@code spotless:check} uses for its error message. Keeps at most a given number of lines, but counts all of them.
 */
final class UnifiedDiff {
	enum Kind {
		/** {@code @@ -1,3 +1,3 @@} */
		HUNK,
		CONTEXT,
		ADDED,
		REMOVED,
		/** {@code \ No newline at end of file} */
		NOTE
	}

	/** One line of the diff, including its leading {@code ' '}, {@code '+'}, {@code '-'}, {@code '@'} or {@code '\'}. */
	static final class Line {
		final Kind kind;
		final String text;

		Line(Kind kind, String text) {
			this.kind = kind;
			this.text = text;
		}
	}

	private final List<Line> lines;
	private final int totalLines;
	private final int added;
	private final int removed;

	private UnifiedDiff(List<Line> lines, int totalLines, int added, int removed) {
		this.lines = Collections.unmodifiableList(lines);
		this.totalLines = totalLines;
		this.added = added;
		this.removed = removed;
	}

	/**
	 * Diffs two texts that use only {@code \n} line endings.
	 *
	 * @param maxLines how many lines of the diff to keep; {@code 0} or less keeps them all
	 */
	static UnifiedDiff compute(String before, String after, int maxLines) {
		RawText a = new RawText(before.getBytes(StandardCharsets.UTF_8));
		RawText b = new RawText(after.getBytes(StandardCharsets.UTF_8));
		EditList edits = new EditList();
		edits.addAll(MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT, a, b));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (DiffFormatter formatter = new DiffFormatter(out)) {
			formatter.format(edits, a, b);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return parse(new String(out.toByteArray(), StandardCharsets.UTF_8), maxLines);
	}

	static UnifiedDiff parse(String unified, int maxLines) {
		List<Line> lines = new ArrayList<>();
		int total = 0;
		int added = 0;
		int removed = 0;
		for (String text : unified.split("\n")) {
			if (text.isEmpty()) {
				continue;
			}
			Kind kind;
			switch (text.charAt(0)) {
			case '@':
				kind = Kind.HUNK;
				break;
			case '+':
				kind = Kind.ADDED;
				added++;
				break;
			case '-':
				kind = Kind.REMOVED;
				removed++;
				break;
			case '\\':
				kind = Kind.NOTE;
				break;
			default:
				kind = Kind.CONTEXT;
				break;
			}
			total++;
			if (maxLines <= 0 || lines.size() < maxLines) {
				lines.add(new Line(kind, text));
			}
		}
		return new UnifiedDiff(lines, total, added, removed);
	}

	/** The lines that were kept, at most {@code maxLines}. */
	List<Line> lines() {
		return lines;
	}

	/** How many lines the whole diff has, including the ones that were not kept. */
	int totalLines() {
		return totalLines;
	}

	int added() {
		return added;
	}

	int removed() {
		return removed;
	}
}
