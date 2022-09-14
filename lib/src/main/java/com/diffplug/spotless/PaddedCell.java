/*
 * Copyright 2016-2022 DiffPlug
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
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Models the result of applying a {@link Formatter} on a given {@link File}
 * while characterizing various failure modes (slow convergence, cycles, and divergence).
 *
 * See {@link #check(Formatter, File)} as the entry point to this class.
 */
public final class PaddedCell {
	/** The kind of result. */
	public enum Type {
		CONVERGE, CYCLE, DIVERGE;

		/** Creates a PaddedCell with the given file and steps. */
		PaddedCell create(File file, List<String> steps) {
			return new PaddedCell(file, this, steps);
		}
	}

	private final File file;
	private final Type type;
	private final List<String> steps;

	private PaddedCell(File file, Type type, List<String> steps) {
		this.file = Objects.requireNonNull(file, "file");
		this.type = Objects.requireNonNull(type, "type");
		// defensive copy
		this.steps = new ArrayList<>(steps);
		requireElementsNonNull(this.steps);
	}

	/** Returns the file which was tested. */
	public File file() {
		return file;
	}

	/** Returns the type of the result (either {@link Type#CONVERGE}, {@link Type#CYCLE}, or {@link Type#DIVERGE}). */
	public Type type() {
		return type;
	}

	/** Returns the steps it takes to get to the result. */
	public List<String> steps() {
		return Collections.unmodifiableList(steps);
	}

	/**
	 * Applies the given formatter to the given file, checking that
	 * F(F(input)) == F(input).
	 *
	 * If it meets this test, {@link #misbehaved()} will return false.
	 *
	 * If it fails the test, {@link #misbehaved()} will return true, and you can find
	 * out more about the misbehavior based on its {@link Type}.
	 *
	 */
	public static PaddedCell check(Formatter formatter, File file) {
		Objects.requireNonNull(formatter, "formatter");
		Objects.requireNonNull(file, "file");
		byte[] rawBytes = ThrowingEx.get(() -> Files.readAllBytes(file.toPath()));
		String raw = new String(rawBytes, formatter.getEncoding());
		String original = LineEnding.toUnix(raw);
		return check(formatter, file, original, MAX_CYCLE);
	}

	public static PaddedCell check(Formatter formatter, File file, String originalUnix) {
		return check(
				Objects.requireNonNull(formatter, "formatter"),
				Objects.requireNonNull(file, "file"),
				Objects.requireNonNull(originalUnix, "originalUnix"),
				MAX_CYCLE);
	}

	private static final int MAX_CYCLE = 10;

	private static PaddedCell check(Formatter formatter, File file, String original, int maxLength) {
		if (maxLength < 2) {
			throw new IllegalArgumentException("maxLength must be at least 2");
		}
		String appliedOnce = formatter.compute(original, file);
		if (appliedOnce.equals(original)) {
			return Type.CONVERGE.create(file, Collections.singletonList(appliedOnce));
		}

		String appliedTwice = formatter.compute(appliedOnce, file);
		if (appliedOnce.equals(appliedTwice)) {
			return Type.CONVERGE.create(file, Collections.singletonList(appliedOnce));
		}

		List<String> appliedN = new ArrayList<>();
		appliedN.add(appliedOnce);
		appliedN.add(appliedTwice);
		String input = appliedTwice;
		while (appliedN.size() < maxLength) {
			String output = formatter.compute(input, file);
			if (output.equals(input)) {
				return Type.CONVERGE.create(file, appliedN);
			} else {
				int idx = appliedN.indexOf(output);
				if (idx >= 0) {
					return Type.CYCLE.create(file, appliedN.subList(idx, appliedN.size()));
				} else {
					appliedN.add(output);
					input = output;
				}
			}
		}
		return Type.DIVERGE.create(file, appliedN);
	}

	/**
	 * Returns true iff the formatter misbehaved in any way
	 * (did not converge after a single iteration).
	 */
	public boolean misbehaved() {
		boolean isWellBehaved = type == Type.CONVERGE && steps.size() <= 1;
		return !isWellBehaved;
	}

	/** Any result which doesn't diverge can be resolved. */
	public boolean isResolvable() {
		return type != Type.DIVERGE;
	}

	/** Returns the "canonical" form for this particular result (only possible if isResolvable). */
	public String canonical() {
		// @formatter:off
		switch (type) {
		case CONVERGE:	return steps.get(steps.size() - 1);
		case CYCLE:		return Collections.min(steps, Comparator.comparing(String::length).thenComparing(Function.identity()));
		case DIVERGE:	throw new IllegalArgumentException("No canonical form for a diverging result");
		default:	throw new IllegalArgumentException("Unknown type: " + type);
		}
		// @formatter:on
	}

	/** Returns a string which describes this result. */
	public String userMessage() {
		// @formatter:off
		switch (type) {
		case CONVERGE:	return "converges after " + steps.size() + " steps";
		case CYCLE:		return "cycles between " + steps.size() + " steps";
		case DIVERGE:	return "diverges after " + steps.size() + " steps";
		default:	throw new IllegalArgumentException("Unknown type: " + type);
		}
		// @formatter:on
	}

	/**
	 * Calculates whether the given file is dirty according to a PaddedCell invocation of the given formatter.
	 * DirtyState includes the clean state of the file, as well as a warning if we were not able to apply the formatter
	 * due to diverging idempotence.
	 */
	public static DirtyState calculateDirtyState(Formatter formatter, File file) throws IOException {
		Objects.requireNonNull(formatter, "formatter");
		Objects.requireNonNull(file, "file");

		byte[] rawBytes = Files.readAllBytes(file.toPath());
		return calculateDirtyState(formatter, file, rawBytes);
	}

	public static DirtyState calculateDirtyState(Formatter formatter, File file, byte[] rawBytes) throws IOException {
		String raw = new String(rawBytes, formatter.getEncoding());
		// check that all characters were encodable
		String encodingError = EncodingErrorMsg.msg(raw, rawBytes, formatter.getEncoding());
		if (encodingError != null) {
			throw new IllegalArgumentException(encodingError);
		}
		String rawUnix = LineEnding.toUnix(raw);

		// enforce the format
		String formattedUnix = formatter.compute(rawUnix, file);
		// convert the line endings if necessary
		String formatted = formatter.computeLineEndings(formattedUnix, file);

		// if F(input) == input, then the formatter is well-behaving and the input is clean
		byte[] formattedBytes = formatted.getBytes(formatter.getEncoding());
		if (Arrays.equals(rawBytes, formattedBytes)) {
			return isClean;
		}

		// F(input) != input, so we'll do a padded check
		String doubleFormattedUnix = formatter.compute(formattedUnix, file);
		if (doubleFormattedUnix.equals(formattedUnix)) {
			// most dirty files are idempotent-dirty, so this is a quick-short circuit for that common case
			return new DirtyState(formattedBytes);
		}

		PaddedCell cell = PaddedCell.check(formatter, file, rawUnix);
		if (!cell.isResolvable()) {
			return didNotConverge;
		}

		// get the canonical bytes
		String canonicalUnix = cell.canonical();
		String canonical = formatter.computeLineEndings(canonicalUnix, file);
		byte[] canonicalBytes = canonical.getBytes(formatter.getEncoding());
		if (!Arrays.equals(rawBytes, canonicalBytes)) {
			// and write them to disk if needed
			return new DirtyState(canonicalBytes);
		} else {
			return isClean;
		}
	}

	/**
	 * The clean/dirty state of a single file.  Intended use:
	 * - {@link #isClean()} means that the file is clean, and there's nothing else to say
	 * - {@link #didNotConverge()} means that we were unable to determine a clean state
	 * - once you've tested the above conditions and you know that it's a dirty file with a converged state,
	 *   then you can call {@link #writeCanonicalTo(OutputStream)} to get the canonical form of the given file.
	 */
	public static class DirtyState {
		@Nullable
		private final byte[] canonicalBytes;

		private DirtyState(@Nullable byte[] canonicalBytes) {
			this.canonicalBytes = canonicalBytes;
		}

		public boolean isClean() {
			return this == isClean;
		}

		public boolean didNotConverge() {
			return this == didNotConverge;
		}

		private byte[] canonicalBytes() {
			if (canonicalBytes == null) {
				throw new IllegalStateException("First make sure that {@code !isClean()} and {@code !didNotConverge()}");
			}
			return canonicalBytes;
		}

		public void writeCanonicalTo(File file) throws IOException {
			Files.write(file.toPath(), canonicalBytes());
		}

		public void writeCanonicalTo(OutputStream out) throws IOException {
			out.write(canonicalBytes());
		}
	}

	/** Returns the DirtyState which corresponds to {@code isClean()}. */
	public static DirtyState isClean() {
		return isClean;
	}

	private static final DirtyState didNotConverge = new DirtyState(null);
	private static final DirtyState isClean = new DirtyState(null);
}
