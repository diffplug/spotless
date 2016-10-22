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
import com.diffplug.common.base.Unhandled;
import com.diffplug.common.collect.ImmutableSet;

class PaddedCell {
	private final Formatter formatter;
	private final File file;
	private final String original;

	PaddedCell(Formatter formatter, File file) {
		this.formatter = formatter;
		this.file = file;
		byte[] rawBytes = Errors.rethrow().get(() -> Files.readAllBytes(file.toPath()));
		String raw = new String(rawBytes, StandardCharsets.UTF_8);
		this.original = LineEnding.toUnix(raw);
	}

	private static final int MAX_CYCLE = 10;

	public Result cycle() {
		return cycle(MAX_CYCLE);
	}

	public Result cycle(int maxLength) {
		Preconditions.checkArgument(maxLength >= 2, "maxLength must be at least 2");
		String appliedOnce = formatter.applyAll(original, file);
		if (appliedOnce.equals(original)) {
			return Result.converges(file, appliedOnce, 0);
		}

		String appliedTwice = formatter.applyAll(appliedOnce, file);
		if (appliedOnce.equals(appliedTwice)) {
			return Result.converges(file, appliedOnce, 1);
		}

		List<String> appliedN = new ArrayList<>();
		appliedN.add(appliedOnce);
		appliedN.add(appliedTwice);
		String input = appliedTwice;
		while (appliedN.size() < maxLength) {
			String output = formatter.applyAll(input, file);
			if (output.equals(input)) {
				return Result.converges(file, output, appliedN.size());
			} else {
				int idx = appliedN.indexOf(output);
				if (idx >= 0) {
					if (idx == appliedN.size() - 1) {
						return Result.converges(file, output, idx);
					} else {
						return Result.cycle(file, appliedN.subList(idx, appliedN.size()));
					}
				} else {
					appliedN.add(output);
					input = output;
				}
			}
		}
		return Result.diverges(file, appliedN);
	}

	/** Returns what is wrong with each file, grouped by category. */
	public static Map<Class<?>, List<PaddedCell.Result>> check(List<File> problemFiles, Formatter formatter) {
		Map<Class<?>, List<PaddedCell.Result>> byType = problemFiles.stream()
				.map(file -> new PaddedCell(formatter, file).cycle())
				.collect(Collectors.groupingBy(Result::type));
		Preconditions.checkArgument(ImmutableSet.of(Cycle.class, Converge.class, Diverge.class).containsAll(byType.keySet()),
				"Unknown results in " + byType.keySet());
		return byType;
	}

	public static final class Result {
		private final File file;
		private final Object result;

		public static Result cycle(File file, List<String> result) {
			return new Result(file, new Cycle(result));
		}

		public static Result converges(File file, String to, int after) {
			return new Result(file, new Converge(to, after));
		}

		public static Result diverges(File file, List<String> steps) {
			return new Result(file, new Diverge(steps));
		}

		private Result(File file, Object result) {
			this.file = Objects.requireNonNull(file);
			this.result = Objects.requireNonNull(result);
		}

		public <T> T applyCycleConvergeDiverge(Function<Cycle, ? extends T> cycle, Function<Converge, ? extends T> converge, Function<Diverge, ? extends T> diverge) {
			if (result instanceof Cycle) {
				return cycle.apply((Cycle) result);
			} else if (result instanceof Converge) {
				return converge.apply((Converge) result);
			} else if (result instanceof Diverge) {
				return diverge.apply((Diverge) result);
			} else {
				throw Unhandled.classException(result);
			}
		}

		public File file() {
			return file;
		}

		public Class<?> type() {
			return result.getClass();
		}

		@SuppressWarnings("unchecked")
		public <T> T as(Class<T> clazz) {
			if (is(clazz)) {
				return (T) result;
			} else {
				throw new IllegalArgumentException("This is " + result.getClass() + ", not " + clazz);
			}
		}

		public boolean is(Class<?> clazz) {
			return result.getClass().equals(clazz);
		}
	}

	public static final class Cycle {
		private final List<String> result;

		Cycle(List<String> result) {
			// find the min based on length, then alphabetically
			String min = Collections.min(result,
					Comparator.comparing(String::length)
							.thenComparing(Function.identity()));
			int minIdx = result.indexOf(min);
			Collections.rotate(result, -minIdx);
			this.result = result;
		}

		public List<String> getCycle() {
			return result;
		}
	}

	public static final class Converge {
		private final String convergesTo;
		private final int after;

		Converge(String convergesTo, int after) {
			this.convergesTo = convergesTo;
			this.after = after;
		}

		public String convergesTo() {
			return convergesTo;
		}

		public int afterIterations() {
			return after;
		}
	}

	public static final class Diverge {
		private final List<String> steps;

		Diverge(List<String> steps) {
			this.steps = steps;
		}

		public List<String> steps() {
			return steps;
		}
	}
}
