/*
 * Copyright 2016-2025 DiffPlug
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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Models the result of applying a {@link Formatter} on a given {@link File}
 * while characterizing various failure modes (slow convergence, cycles, and divergence).
 * <p>
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
	 * <p>
	 * If it meets this test, {@link #misbehaved()} will return false.
	 * <p>
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
		return check(formatter, file, original, MAX_CYCLE, new ValuePerStep<>(formatter));
	}

	public static PaddedCell check(Formatter formatter, File file, String originalUnix) {
		return check(formatter, file, originalUnix, new ValuePerStep<>(formatter));
	}

	public static PaddedCell check(Formatter formatter, File file, String originalUnix, ValuePerStep<Throwable> exceptionPerStep) {
		return check(formatter, file, originalUnix, MAX_CYCLE, exceptionPerStep);
	}

	private static final int MAX_CYCLE = 10;

	private static PaddedCell check(Formatter formatter, File file, String original, int maxLength, ValuePerStep<Throwable> exceptionPerStep) {
		if (maxLength < 2) {
			throw new IllegalArgumentException("maxLength must be at least 2");
		}
		String appliedOnce = formatter.computeWithLint(original, file, exceptionPerStep);
		if (appliedOnce.equals(original)) {
			return Type.CONVERGE.create(file, List.of(appliedOnce));
		}

		String appliedTwice = formatter.computeWithLint(appliedOnce, file, exceptionPerStep);
		if (appliedOnce.equals(appliedTwice)) {
			return Type.CONVERGE.create(file, List.of(appliedOnce));
		}

		List<String> appliedN = new ArrayList<>();
		appliedN.add(appliedOnce);
		appliedN.add(appliedTwice);
		String input = appliedTwice;
		while (appliedN.size() < maxLength) {
			String output = formatter.computeWithLint(input, file, exceptionPerStep);
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
		return switch (type) {
			case CONVERGE -> steps.get(steps.size() - 1);
			case CYCLE ->
				Collections.min(steps, Comparator.comparingInt(String::length).thenComparing(Function.identity()));
			case DIVERGE -> throw new IllegalArgumentException("No canonical form for a diverging result");
		};
	}

	/** Returns a string which describes this result. */
	public String userMessage() {
		return switch (type) {
			case CONVERGE -> "converges after " + steps.size() + " steps";
			case CYCLE -> "cycles between " + steps.size() + " steps";
			case DIVERGE -> "diverges after " + steps.size() + " steps";
		};
	}
}
