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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Unhandled;

class PaddedCell {
	private final Formatter formatter;
	private final File file;
	private final String original;

	PaddedCell(Formatter formatter, File file) throws IOException {
		this.formatter = formatter;
		this.file = file;
		byte[] rawBytes = Files.readAllBytes(file.toPath());
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
			return Result.converges(appliedOnce, 0);
		}

		String appliedTwice = formatter.applyAll(appliedOnce, file);
		if (appliedOnce.equals(appliedTwice)) {
			return Result.converges(appliedOnce, 1);
		}

		List<String> appliedN = new ArrayList<>();
		appliedN.add(appliedOnce);
		appliedN.add(appliedTwice);
		String input = appliedTwice;
		while (appliedN.size() < maxLength) {
			String output = formatter.applyAll(input, file);
			if (output.equals(input)) {
				return Result.converges(output, appliedN.size());
			} else {
				int idx = appliedN.indexOf(output);
				if (idx >= 0) {
					if (idx == appliedN.size() - 1) {
						return Result.converges(output, idx);
					} else {
						return Result.cycle(appliedN.subList(idx, appliedN.size()));
					}
				} else {
					appliedN.add(output);
					input = output;
				}
			}
		}
		return Result.diverges(maxLength);
	}

	public static final class Result {
		private final Object result;

		public static Result cycle(List<String> result) {
			return new Result(new Cycle(result));
		}

		public static Result converges(String to, int after) {
			return new Result(new Converge(to, after));
		}

		public static Result diverges(int after) {
			return new Result(new Diverge(after));
		}

		private Result(Object result) {
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
		private final int after;

		Diverge(int after) {
			this.after = after;
		}

		public int afterIterations() {
			return after;
		}
	}
}
