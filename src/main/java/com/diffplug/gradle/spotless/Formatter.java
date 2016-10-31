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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/** Formatter which performs the full formatting. */
public final class Formatter {
	final LineEnding.Policy lineEndingsPolicy;
	final Charset encoding;
	final Path projectDirectory;
	final List<FormatterStep> steps;

	private static final Logger logger = Logging.getLogger(Formatter.class);

	/** It's important to specify the charset. */
	@Deprecated
	public Formatter(LineEnding.Policy lineEndingsPolicy, Path projectDirectory, List<FormatterStep> steps) {
		this(lineEndingsPolicy, StandardCharsets.UTF_8, projectDirectory, steps);
	}

	/**
	 * The number of required parameters is starting to get difficult to use. Use
	 * {@link Formatter#builder()} instead.
	 */
	@Deprecated
	public Formatter(LineEnding.Policy lineEndingsPolicy, Charset encoding, Path projectDirectory, List<FormatterStep> steps) {
		this.lineEndingsPolicy = Objects.requireNonNull(lineEndingsPolicy, "lineEndingsPolicy");
		this.encoding = Objects.requireNonNull(encoding, "encoding");
		this.projectDirectory = Objects.requireNonNull(projectDirectory, "projectDirectory");
		this.steps = new ArrayList<>(Objects.requireNonNull(steps, "steps"));
	}

	public static Formatter.Builder builder() {
		return new Formatter.Builder();
	}

	public static class Builder {
		// required parameters
		private LineEnding.Policy lineEndingsPolicy;
		private Charset encoding;
		private Path projectDirectory;
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

		public Builder projectDirectory(Path projectDirectory) {
			this.projectDirectory = projectDirectory;
			return this;
		}

		public Builder steps(List<FormatterStep> steps) {
			this.steps = steps;
			return this;
		}

		public Formatter build() {
			return new Formatter(lineEndingsPolicy, encoding, projectDirectory, steps);
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
	String applyLineEndings(String unix, File file) {
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
	String applySteps(String unix, File file) {
		for (FormatterStep step : steps) {
			try {
				String formatted = step.format(unix, file);
				// should already be unix-only, but
				// some steps might misbehave
				unix = LineEnding.toUnix(formatted);
			} catch (Throwable e) {
				logger.warn("Unable to apply step " + step.getName() + " to " + projectDirectory.relativize(file.toPath()) + ": " + e.getMessage());
				logger.info("Exception is ", e);
			}
		}
		return unix;
	}
}
