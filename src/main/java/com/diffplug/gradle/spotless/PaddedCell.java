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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;

/**
 * Models the result of applying a {@link Formatter} on a given {@link File}
 * while characterizing various failure modes (slow convergence, cycles, and divergence).
 *
 * See {@link #check(Formatter, File)} as the entry point to this class.
 */
public class PaddedCell {
	/**
	 * Applies the given formatter to the given file, checking that
	 * F(F(input)) == F(input).
	 *
	 * If it meets this test, {@link #misbehaved()} will return false.
	 *
	 * If it fails the test, {@link #misbehaved()} will return true, and you can find
	 * out more about the misbehavior through either the {@link Converge}, {@link Cycle},
	 * or {@link Diverge} classes.
	 *
	 * If the result converged eventually, then `is(Converge.class)` will be true, and you
	 * can find out what it converged to and how long it took using `as(Converge.class)`.
	 *
	 * @param formatter
	 * @param file
	 * @return
	 */
	public static PaddedCell check(Formatter formatter, File file) {
		return check(formatter, file, MAX_CYCLE);
	}

	private static final int MAX_CYCLE = 10;

	private static PaddedCell check(Formatter formatter, File file, int maxLength) {
		byte[] rawBytes = Errors.rethrow().get(() -> Files.readAllBytes(file.toPath()));
		String raw = new String(rawBytes, StandardCharsets.UTF_8);
		String original = LineEnding.toUnix(raw);

		Preconditions.checkArgument(maxLength >= 2, "maxLength must be at least 2");
		String appliedOnce = formatter.applyAll(original, file);
		if (appliedOnce.equals(original)) {
			return converges(file, Collections.singletonList(appliedOnce));
		}

		String appliedTwice = formatter.applyAll(appliedOnce, file);
		if (appliedOnce.equals(appliedTwice)) {
			return converges(file, Collections.singletonList(appliedOnce));
		}

		List<String> appliedN = new ArrayList<>();
		appliedN.add(appliedOnce);
		appliedN.add(appliedTwice);
		String input = appliedTwice;
		while (appliedN.size() < maxLength) {
			String output = formatter.applyAll(input, file);
			if (output.equals(input)) {
				return converges(file, appliedN);
			} else {
				int idx = appliedN.indexOf(output);
				if (idx >= 0) {
					return cycle(file, appliedN.subList(idx, appliedN.size()));
				} else {
					appliedN.add(output);
					input = output;
				}
			}
		}
		return diverges(file, appliedN);
	}

	/** Returns what is wrong with each file, grouped by category. */
	public static Map<ResultType, List<PaddedCell>> checkMisbehaves(Formatter formatter, List<File> problemFiles) {
		return problemFiles.stream()
				.map(file -> PaddedCell.check(formatter, file))
				.filter(PaddedCell::misbehaved)
				.collect(Collectors.groupingBy(PaddedCell::type));
	}

	private final File file;
	private final ResultType type;
	private final List<String> steps;

	static PaddedCell cycle(File file, List<String> steps) {
		// find the min based on length, then alphabetically
		String min = Collections.min(steps,
				Comparator.comparing(String::length)
						.thenComparing(Function.identity()));
		int minIdx = steps.indexOf(min);
		Collections.rotate(steps, -minIdx);
		return new PaddedCell(file, ResultType.CYCLE, steps);
	}

	static PaddedCell converges(File file, List<String> steps) {
		return new PaddedCell(file, ResultType.CONVERGE, steps);
	}

	static PaddedCell diverges(File file, List<String> steps) {
		return new PaddedCell(file, ResultType.DIVERGE, steps);
	}

	private PaddedCell(File file, ResultType type, List<String> steps) {
		this.file = Objects.requireNonNull(file);
		this.type = Objects.requireNonNull(type);
		this.steps = Objects.requireNonNull(steps);
	}

	/** Returns the file which was tested. */
	public File file() {
		return file;
	}

	/** Returns the type of the result (either {@link Cycle}, {@link Converge}, or {@link Diverge}). */
	public ResultType type() {
		return type;
	}

	/** Returns the steps it takes to get to the result. */
	public List<String> steps() {
		return steps;
	}

	/**
	 * Returns true iff the formatter misbehaved in any way
	 * (did not converge after a single iteration).
	 */
	public boolean misbehaved() {
		boolean isWellBehaved = type == ResultType.CONVERGE && steps.size() <= 1;
		return !isWellBehaved;
	}

	/** The kind of result. */
	public enum ResultType {
		CONVERGE, CYCLE, DIVERGE
	}
}
