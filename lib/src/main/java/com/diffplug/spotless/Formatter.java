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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Formatter which performs the full formatting. */
public final class Formatter implements Serializable, AutoCloseable {
	private static final long serialVersionUID = 1L;

	// The name is used for logging purpose. It does not convey any applicative purpose
	private LineEnding.Policy lineEndingsPolicy;
	private Charset encoding;
	private List<FormatterStep> steps;

	private Formatter(LineEnding.Policy lineEndingsPolicy, Charset encoding, List<FormatterStep> steps) {
		this.lineEndingsPolicy = Objects.requireNonNull(lineEndingsPolicy, "lineEndingsPolicy");
		this.encoding = Objects.requireNonNull(encoding, "encoding");
		this.steps = requireElementsNonNull(new ArrayList<>(steps));
	}

	// override serialize output
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(lineEndingsPolicy);
		out.writeObject(encoding.name());
		out.writeObject(steps);
	}

	// override serialize input
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		lineEndingsPolicy = (LineEnding.Policy) in.readObject();
		encoding = Charset.forName((String) in.readObject());
		steps = (List<FormatterStep>) in.readObject();
	}

	// override serialize input
	@SuppressWarnings("unused")
	private void readObjectNoData() throws ObjectStreamException {
		throw new UnsupportedOperationException();
	}

	public LineEnding.Policy getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	public Charset getEncoding() {
		return encoding;
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

		public Builder steps(List<FormatterStep> steps) {
			this.steps = steps;
			return this;
		}

		public Formatter build() {
			return new Formatter(lineEndingsPolicy, encoding, steps);
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
				// TODO: this is bad, but it won't matter when add support for linting
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				} else {
					throw new RuntimeException(e);
				}
			}
		}
		return unix;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + encoding.hashCode();
		result = prime * result + lineEndingsPolicy.hashCode();
		result = prime * result + steps.hashCode();
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
		return encoding.equals(other.encoding) &&
				lineEndingsPolicy.equals(other.lineEndingsPolicy) &&
				steps.equals(other.steps);
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
