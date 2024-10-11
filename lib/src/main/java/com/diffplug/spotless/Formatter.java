/*
 * Copyright 2016-2024 DiffPlug
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

import static com.diffplug.spotless.LibPreconditions.requireElementsNonNull;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Formatter which performs the full formatting. */
public final class Formatter implements Serializable, AutoCloseable {
	private static final long serialVersionUID = 1L;

	// The name is used for logging purpose. It does not convey any applicative purpose
	private String name;
	private LineEnding.Policy lineEndingsPolicy;
	private Charset encoding;
	private Path rootDir;
	private List<FormatterStep> steps;
	private FormatExceptionPolicy exceptionPolicy;

	private Formatter(String name, LineEnding.Policy lineEndingsPolicy, Charset encoding, Path rootDirectory, List<FormatterStep> steps, FormatExceptionPolicy exceptionPolicy) {
		this.name = name;
		this.lineEndingsPolicy = Objects.requireNonNull(lineEndingsPolicy, "lineEndingsPolicy");
		this.encoding = Objects.requireNonNull(encoding, "encoding");
		this.rootDir = Objects.requireNonNull(rootDirectory, "rootDir");
		this.steps = requireElementsNonNull(new ArrayList<>(steps));
		this.exceptionPolicy = Objects.requireNonNull(exceptionPolicy, "exceptionPolicy");
	}

	// override serialize output
	@SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(name);
		out.writeObject(lineEndingsPolicy);
		out.writeObject(encoding.name());
		out.writeObject(rootDir.toString());
		ConfigurationCacheHack.SERIALIZE_FOR_ROUNDTRIP = true;
		out.writeObject(steps);
		out.writeObject(exceptionPolicy);
	}

	// override serialize input
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		name = (String) in.readObject();
		lineEndingsPolicy = (LineEnding.Policy) in.readObject();
		encoding = Charset.forName((String) in.readObject());
		rootDir = Paths.get((String) in.readObject());
		steps = (List<FormatterStep>) in.readObject();
		exceptionPolicy = (FormatExceptionPolicy) in.readObject();
	}

	// override serialize input
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException();
	}

	public String getName() {
		return name;
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

	public FormatExceptionPolicy getExceptionPolicy() {
		return exceptionPolicy;
	}

	public static Formatter.Builder builder() {
		return new Formatter.Builder();
	}

	public static class Builder {
		// optional parameters
		private String name = "unnamed";
		// required parameters
		private LineEnding.Policy lineEndingsPolicy;
		private Charset encoding;
		private Path rootDir;
		private List<FormatterStep> steps;
		private FormatExceptionPolicy exceptionPolicy;

		private Builder() {}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

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

		public Builder exceptionPolicy(FormatExceptionPolicy exceptionPolicy) {
			this.exceptionPolicy = exceptionPolicy;
			return this;
		}

		public Formatter build() {
			return new Formatter(name, lineEndingsPolicy, encoding, rootDir, steps,
					exceptionPolicy == null ? FormatExceptionPolicy.failOnlyOnError() : exceptionPolicy);
		}
	}

	/** Returns true iff the given file's formatting is up-to-date. */
	public boolean isClean(File file) throws IOException {
		Objects.requireNonNull(file);

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
		String formatted = compute(unix, file);
		// return true iff the formatted string equals the unix one
		return formatted.equals(unix);
	}

	/** Applies formatting to the given file. */
	public void applyTo(File file) throws IOException {
		applyToAndReturnResultIfDirty(file);
	}

	/**
	 * Applies formatting to the given file.
	 * <p>
	 * Returns null if the file was already clean, or the
	 * formatted result with unix newlines if it was not.
	 */
	public @Nullable String applyToAndReturnResultIfDirty(File file) throws IOException {
		Objects.requireNonNull(file);

		byte[] rawBytes = Files.readAllBytes(file.toPath());
		String raw = new String(rawBytes, encoding);
		String rawUnix = LineEnding.toUnix(raw);

		// enforce the format
		String formattedUnix = compute(rawUnix, file);
		// enforce the line endings
		String formatted = computeLineEndings(formattedUnix, file);

		// write out the file iff it has changed
		byte[] formattedBytes = formatted.getBytes(encoding);
		if (!Arrays.equals(rawBytes, formattedBytes)) {
			Files.write(file.toPath(), formattedBytes, StandardOpenOption.TRUNCATE_EXISTING);
			return formattedUnix;
		} else {
			return null;
		}
	}

	/** Applies the appropriate line endings to the given unix content. */
	public String computeLineEndings(String unix, File file) {
		Objects.requireNonNull(unix, "unix");
		Objects.requireNonNull(file, "file");

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
	public String compute(String unix, File file) {
		Objects.requireNonNull(unix, "unix");
		Objects.requireNonNull(file, "file");

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
			} catch (Throwable e) {
				if (file == NO_FILE_SENTINEL) {
					exceptionPolicy.handleError(e, step, "");
				} else {
					// Path may be forged from a different FileSystem than Filesystem.default
					String relativePath = rootDir.relativize(rootDir.getFileSystem().getPath(file.getPath())).toString();
					exceptionPolicy.handleError(e, step, relativePath);
				}
			}
		}
		return unix;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + encoding.hashCode();
		result = prime * result + lineEndingsPolicy.hashCode();
		result = prime * result + rootDir.hashCode();
		result = prime * result + steps.hashCode();
		result = prime * result + exceptionPolicy.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Formatter other = (Formatter) obj;
		return name.equals(other.name) &&
				encoding.equals(other.encoding) &&
				lineEndingsPolicy.equals(other.lineEndingsPolicy) &&
				rootDir.equals(other.rootDir) &&
				steps.equals(other.steps) &&
				exceptionPolicy.equals(other.exceptionPolicy);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void close() {
		for (FormatterStep step : steps) {
			try {
				step.close();
			} catch (Exception e) {
				throw ThrowingEx.asRuntime(e);
			}
		}
	}

	/** This Sentinel reference may be used to pass string content to a Formatter or FormatterStep when there is no actual File to format */
	public static final File NO_FILE_SENTINEL = new File("NO_FILE_SENTINEL");

	static void checkNotSentinel(File file) {
		if (file == Formatter.NO_FILE_SENTINEL) {
			throw new IllegalArgumentException("This step requires the underlying file. If this is a test, use StepHarnessWithFile");
		}
	}
}
