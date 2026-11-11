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
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.doxia.sink.SinkFactory;

import com.diffplug.spotless.DirtyState;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.Lint;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.generic.FenceStep;

/**
 * What Spotless found in a project, collected one format at a time, and rendered as Maven site pages:
 * a summary page plus one page for each file that needs formatting, has lint problems, does not converge or could
 * not be checked.
 */
public final class SpotlessReport {
	private final int maxDiffLines;
	private final List<Format> formats = new ArrayList<>();
	private final Set<String> formatNames = new HashSet<>();

	/** @param maxDiffLines the most lines of diff kept for each file; {@code 0} or less keeps whole diffs */
	public SpotlessReport(int maxDiffLines) {
		this.maxDiffLines = maxDiffLines;
	}

	/**
	 * Starts collecting results for one format, such as {@code <java>}.
	 *
	 * @param factoryName the simple class name of the format's factory, e.g. {@code Java} or {@code Format}
	 */
	public Format addFormat(String factoryName, Formatter formatter) {
		String base = factoryName.isEmpty() ? "format" : Character.toLowerCase(factoryName.charAt(0)) + factoryName.substring(1);
		String name = base;
		for (int i = 2; formatNames.contains(name); i++) {
			name = base + " #" + i;
		}
		formatNames.add(name);
		List<String> steps = new ArrayList<>();
		for (FormatterStep step : formatter.getSteps()) {
			steps.add(step.getName());
		}
		if (steps.size() == 1 && steps.get(0).equals(FenceStep.defaultToggleName())) {
			// <toggleOffOn/> wraps all the steps in one fence step, which hides their names
			steps.set(0, FenceStep.defaultToggleName() + " (spotless:off and spotless:on around the configured steps)");
		}
		Format format = new Format(name, steps, formatter.getEncoding(), maxDiffLines);
		formats.add(format);
		return format;
	}

	List<Format> formats() {
		return Collections.unmodifiableList(formats);
	}

	/** A one-line summary for the build log, e.g. {@code Spotless report: 3 files, 1 needs formatting.} */
	public String summary() {
		int files = 0;
		int needFormatting = 0;
		int withLints = 0;
		int notConverging = 0;
		int errors = 0;
		for (Format format : formats) {
			for (FileResult file : format.files) {
				files++;
				needFormatting += file.changes != null ? 1 : 0;
				withLints += file.lints.isEmpty() ? 0 : 1;
				notConverging += file.didNotConverge ? 1 : 0;
				errors += file.error != null ? 1 : 0;
			}
		}
		StringBuilder summary = new StringBuilder("Spotless report: ").append(count(files, "file"));
		if (needFormatting + withLints + notConverging + errors == 0) {
			return summary.append(files == 0 ? "." : ", all clean.").toString();
		}
		if (needFormatting > 0) {
			summary.append(", ").append(needFormatting).append(needFormatting == 1 ? " needs" : " need").append(" formatting");
		}
		if (withLints > 0) {
			summary.append(", ").append(withLints).append(withLints == 1 ? " has" : " have").append(" lint problems");
		}
		if (notConverging > 0) {
			summary.append(", ").append(notConverging).append(notConverging == 1 ? " does" : " do").append(" not converge");
		}
		if (errors > 0) {
			summary.append(", ").append(errors).append(" could not be checked");
		}
		return summary.append('.').toString();
	}

	static String count(int n, String noun) {
		return n + " " + noun + (n == 1 ? "" : "s");
	}

	/**
	 * Renders the report for {@code mvn site}.
	 *
	 * @param mainSink the sink for the summary page, named {@code outputName + ".html"}
	 * @param sinkFactory creates the per-file pages next to the summary page; if {@code null}, the details go on the summary page
	 * @param outputDirectory the directory {@code sinkFactory} writes pages to
	 */
	public void render(Sink mainSink, SinkFactory sinkFactory, File outputDirectory, String outputName) throws IOException {
		ReportRenderer.PageSinks pages = sinkFactory == null ? null : fileName -> sinkFactory.createSink(outputDirectory, fileName);
		new ReportRenderer(this, outputName).render(mainSink, pages);
	}

	/**
	 * Writes the report as self-contained HTML pages, for {@code mvn spotless:report} outside of {@code mvn site}.
	 *
	 * @return the summary page
	 */
	public File writeStandalone(File outputDirectory, String outputName) throws IOException {
		Path directory = outputDirectory.toPath();
		Files.createDirectories(directory);
		ReportRenderer.PageSinks pages = fileName -> HtmlSink.create(Files.newBufferedWriter(directory.resolve(fileName), StandardCharsets.UTF_8));
		String mainPage = outputName + ".html";
		try {
			new ReportRenderer(this, outputName).render(pages.open(mainPage), pages);
		} catch (UncheckedIOException e) {
			// HtmlSink can only report write failures unchecked
			throw e.getCause();
		}
		return directory.resolve(mainPage).toFile();
	}

	/** The files of one format, such as {@code <java>}. */
	public static final class Format {
		final String name;
		final List<String> steps;
		final Charset encoding;
		final List<FileResult> files = new ArrayList<>();
		private final int maxDiffLines;

		private Format(String name, List<String> steps, Charset encoding, int maxDiffLines) {
			this.name = name;
			this.steps = List.copyOf(steps);
			this.encoding = encoding;
			this.maxDiffLines = maxDiffLines;
		}

		/**
		 * Records a file that Spotless checked.
		 *
		 * @param path the file's path relative to the project directory, with {@code /} separators
		 * @param rawBytes the file's content when {@code lintState} was computed
		 */
		public void addFile(String path, LintState lintState, byte[] rawBytes, Formatter formatter) throws IOException {
			DirtyState dirty = lintState.getDirtyState();
			Changes changes = null;
			if (!dirty.isClean() && !dirty.didNotConverge()) {
				ByteArrayOutputStream canonical = new ByteArrayOutputStream();
				dirty.writeCanonicalTo(canonical);
				Charset charset = formatter.getEncoding();
				changes = Changes.between(new String(rawBytes, charset), new String(canonical.toByteArray(), charset), maxDiffLines);
			}
			List<LintRow> lints = new ArrayList<>();
			if (lintState.isHasLints()) {
				for (Map.Entry<String, List<Lint>> entry : lintState.getLintsByStep(formatter).entrySet()) {
					for (Lint lint : entry.getValue()) {
						lints.add(new LintRow(entry.getKey(), lint.getLineStart(), lint.getLineEnd(), lint.getShortCode(), lint.getDetail()));
					}
				}
			}
			files.add(new FileResult(path, changes, lints, dirty.didNotConverge(), null));
		}

		/** Records a file that Spotless could not check, for example because it does not decode in the format's encoding. */
		public void addError(String path, Throwable error) {
			String message = error.getMessage() == null ? error.getClass().getName() : error.getMessage();
			files.add(new FileResult(path, null, List.of(), false, message));
		}
	}

	/** What Spotless found in one file. */
	static final class FileResult {
		final String path;
		/** {@code null} if the file is already formatted. */
		final Changes changes;
		final List<LintRow> lints;
		final boolean didNotConverge;
		/** {@code null} unless the file could not be checked. */
		final String error;

		FileResult(String path, Changes changes, List<LintRow> lints, boolean didNotConverge, String error) {
			this.path = path;
			this.changes = changes;
			this.lints = List.copyOf(lints);
			this.didNotConverge = didNotConverge;
			this.error = error;
		}

		boolean isClean() {
			return changes == null && lints.isEmpty() && !didNotConverge && error == null;
		}
	}

	/** One lint problem, as shown in the tables. */
	static final class LintRow {
		final String step;
		final int lineStart;
		final int lineEnd;
		final String code;
		final String detail;

		LintRow(String step, int lineStart, int lineEnd, String code, String detail) {
			this.step = step;
			this.lineStart = lineStart;
			this.lineEnd = lineEnd;
			this.code = code;
			this.detail = detail;
		}

		String lines() {
			if (lineStart == Lint.LINE_UNDEFINED) {
				return "";
			}
			return lineStart == lineEnd ? Integer.toString(lineStart) : lineStart + "-" + lineEnd;
		}

		String firstLineOfDetail() {
			int newline = detail.indexOf('\n');
			return newline == -1 ? detail : detail.substring(0, newline);
		}
	}

	/** The changes {@code spotless:apply} would make to one file. */
	static final class Changes {
		/** A sentence about invisible changes such as line endings, or {@code null}. */
		final String note;
		/** {@code null} when only invisible characters change. */
		final UnifiedDiff diff;

		private Changes(String note, UnifiedDiff diff) {
			this.note = note;
			this.diff = diff;
		}

		static Changes between(String raw, String formatted, int maxDiffLines) {
			String rawUnix = LineEnding.toUnix(raw);
			String formattedUnix = LineEnding.toUnix(formatted);
			String actual = lineEndings(raw);
			String expected = lineEndings(formatted);
			// adding or removing the only line break shows up in the diff, it isn't a change of line endings
			boolean lineEndingsChange = !actual.equals(expected) && !actual.equals(NO_LINE_BREAKS) && !expected.equals(NO_LINE_BREAKS);
			if (rawUnix.equals(formattedUnix)) {
				if (lineEndingsChange) {
					return new Changes("Only the line endings change: the file uses " + actual + ", but Spotless expects " + expected + ".", null);
				}
				return new Changes("Only invisible characters change, for example the encoding of some characters.", null);
			}
			String note = lineEndingsChange ? "The line endings also change: the file uses " + actual + ", but Spotless expects " + expected + "." : null;
			return new Changes(note, UnifiedDiff.compute(rawUnix, formattedUnix, maxDiffLines));
		}

		static String lineEndings(String text) {
			boolean crlf = false;
			boolean lf = false;
			boolean cr = false;
			for (int i = 0; i < text.length(); i++) {
				char c = text.charAt(i);
				if (c == '\r') {
					if (i + 1 < text.length() && text.charAt(i + 1) == '\n') {
						crlf = true;
						i++;
					} else {
						cr = true;
					}
				} else if (c == '\n') {
					lf = true;
				}
			}
			int kinds = (crlf ? 1 : 0) + (lf ? 1 : 0) + (cr ? 1 : 0);
			if (kinds > 1) {
				return "mixed line endings";
			} else if (crlf) {
				return "CRLF (\\r\\n)";
			} else if (cr) {
				return "CR (\\r)";
			} else if (lf) {
				return "LF (\\n)";
			} else {
				return NO_LINE_BREAKS;
			}
		}

		private static final String NO_LINE_BREAKS = "no line breaks";
	}
}
