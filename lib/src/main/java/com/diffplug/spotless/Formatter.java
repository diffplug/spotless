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
package com.diffplug.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Formatter which performs the full formatting. */
public final class Formatter {
	private final LineEnding.Policy lineEndingsPolicy;
	private final Charset encoding;
	private final Path rootDir;
	private final List<FormatterStep> steps;

	private static final Logger logger = Logger.getLogger(Formatter.class.getName());

	private Formatter(LineEnding.Policy lineEndingsPolicy, Charset encoding, Path rootDirectory, List<FormatterStep> steps) {
		this.lineEndingsPolicy = Objects.requireNonNull(lineEndingsPolicy, "lineEndingsPolicy");
		this.encoding = Objects.requireNonNull(encoding, "encoding");
		this.rootDir = Objects.requireNonNull(rootDirectory, "rootDir");
		this.steps = new ArrayList<>(Objects.requireNonNull(steps, "steps"));
	}

	public LineEnding.Policy getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	public Charset getEncoding() {
		return encoding;
	}

	public Path getRootDir() {
		return rootDir;
	}

	public List<FormatterStep> getSteps() {
		return steps;
	}

	public static Formatter.Builder builder() {
		return new Formatter.Builder();
	}

	public static class Builder {
		// required parameters
		private LineEnding.Policy lineEndingsPolicy;
		private Charset encoding;
		private Path rootDir;
		private List<FormatterStep> steps;

		private Builder() {}

		public Builder lineEndingsPolicy(LineEnding.Policy lineEndingsPolicy) {
			this.lineEndingsPolicy = lineEndingsPolicy;
			return this;
		}

		public Builder encoding(Charset encoding) {
			this.encoding = encoding;
			return this;
		}

		public Builder rootDir(Path rootDir) {
			this.rootDir = rootDir;
			return this;
		}

		public Builder steps(List<FormatterStep> steps) {
			this.steps = steps;
			return this;
		}

		public Formatter build() {
			return new Formatter(lineEndingsPolicy, encoding, rootDir, steps);
		}
	}

	/** Returns true iff the given file's formatting is up-to-date. */
	public boolean isClean(File file) throws IOException {
		String raw = new String(Files.readAllBytes(file.toPath()), encoding);
		String unix = LineEnding.toUnix(raw);

		// check the newlines (we can find these problems without even running the steps)
		int totalNewLines = (int) unix.codePoints().filter(val -> val == '\n').count();
		int windowsNewLines = raw.length() - unix.length();
		if (lineEndingsPolicy.isUnix(file)) {
			if (windowsNewLines != 0) {
				return false;
			}
		} else {
			if (windowsNewLines != totalNewLines) {
				return false;
			}
		}

		// check the other formats
		String formatted = applySteps(unix, file);

		// return true iff the formatted string equals the unix one
		return formatted.equals(unix);
	}

	/** Applies formatting to the given file. */
	public void applyFormat(File file) throws IOException {
		byte[] rawBytes = Files.readAllBytes(file.toPath());
		String raw = new String(rawBytes, encoding);
		String rawUnix = LineEnding.toUnix(raw);

		// enforce the format
		String formattedUnix = applySteps(rawUnix, file);
		// enforce the line endings
		String formatted = applyLineEndings(formattedUnix, file);

		// write out the file iff it has changed
		byte[] formattedBytes = formatted.getBytes(encoding);
		if (!Arrays.equals(rawBytes, formattedBytes)) {
			Files.write(file.toPath(), formattedBytes, StandardOpenOption.TRUNCATE_EXISTING);
		}
	}

	/** Applies the appropriate line endings to the given unix content. */
	public String applyLineEndings(String unix, File file) {
		String ending = lineEndingsPolicy.getEndingFor(file);
		if (!ending.equals(LineEnding.UNIX.str())) {
			return unix.replace(LineEnding.UNIX.str(), ending);
		} else {
			return unix;
		}
	}

	/**
	 * Returns the result of calling all of the FormatterSteps.
	 * The input must have unix line endings, and the output
	 * is guaranteed to also have unix line endings.
	 */
	public String applySteps(String unix, File file) throws Error {
		for (FormatterStep step : steps) {
			try {
				String formatted = step.format(unix, file);
				if (formatted == null) {
					// This probably means it was a step that only checks
					// for errors and doesn't actually have any fixes.
					// No exception was thrown so we can just continue.
				} else {
					// Should already be unix-only, but some steps might misbehave.
					unix = LineEnding.toUnix(formatted);
				}
			} catch (Error e) {
				logger.severe("Step '" + step.getName() + "' found problem in '" + rootDir.relativize(file.toPath()) + "':\n" + e.getMessage());
				throw e;
			} catch (Throwable e) {
				logger.warning("Unable to apply step '" + step.getName() + "' to '" + rootDir.relativize(file.toPath()) + "': " + e.getMessage());
				logger.log(Level.FINE, "Exception is ", e);
			}
		}
		return unix;
	}

	/**
	 * Hint that any resources which were opened by the steps can probably be closed now.
	 */
	public void finish() {
		for (FormatterStep step : steps) {
			step.finish();
		}
	}
}
