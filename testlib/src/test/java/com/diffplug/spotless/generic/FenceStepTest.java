/*
 * Copyright 2020-2024 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.File;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterFunc;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;

class FenceStepTest extends ResourceHarness {
	@Test
	void single() {
		FormatterStep fence = FenceStep.named("fence").openClose("spotless:off", "spotless:on")
				.preserveWithin(Arrays.asList(createNeverUpToDateSerializable("lowercase", String::toLowerCase)));
		StepHarness harness = StepHarness.forStepNoRoundtrip(fence);
		harness.test(
				StringPrinter.buildStringFromLines(
						"A B C",
						"spotless:off",
						"D E F",
						"spotless:on",
						"G H I"),
				StringPrinter.buildStringFromLines(
						"a b c",
						"spotless:off",
						"D E F",
						"spotless:on",
						"g h i"));
	}

	@Test
	void multiple() {
		FormatterStep fence = FenceStep.named("fence").openClose("spotless:off", "spotless:on")
				.preserveWithin(Arrays.asList(createNeverUpToDateSerializable("lowercase", String::toLowerCase)));
		StepHarness harness = StepHarness.forStepNoRoundtrip(fence);
		harness.test(
				StringPrinter.buildStringFromLines(
						"A B C",
						"spotless:off",
						"D E F",
						"spotless:on",
						"G H I",
						"spotless:off J K L spotless:on",
						"M N O",
						"P Q R",
						"S T U spotless:off V W",
						" X ",
						" Y spotless:on Z",
						"1 2 3"),
				StringPrinter.buildStringFromLines(
						"a b c",
						"spotless:off",
						"D E F",
						"spotless:on",
						"g h i",
						"spotless:off J K L spotless:on",
						"m n o",
						"p q r",
						"s t u spotless:off V W",
						" X ",
						" Y spotless:on z",
						"1 2 3"));
	}

	@Test
	void broken() {
		FormatterStep fence = FenceStep.named("fence").openClose("spotless:off", "spotless:on")
				.preserveWithin(Arrays.asList(createNeverUpToDateSerializable("uppercase", String::toUpperCase)));
		// this fails because uppercase turns spotless:off into SPOTLESS:OFF, etc
		StepHarness.forStepNoRoundtrip(fence).testExceptionMsg(StringPrinter.buildStringFromLines("A B C",
				"spotless:off",
				"D E F",
				"spotless:on",
				"G H I")).isEqualTo("An intermediate step removed a match of spotless:off spotless:on");
	}

	@Test
	void andApply() {
		FormatterStep fence = FenceStep.named("fence").openClose("<lower>", "</lower>")
				.applyWithin(Arrays.asList(createNeverUpToDateSerializable("lowercase", String::toLowerCase)));
		StepHarness.forStepNoRoundtrip(fence).test(
				StringPrinter.buildStringFromLines(
						"A B C",
						"<lower>",
						"D E F",
						"</lower>",
						"G H I"),
				StringPrinter.buildStringFromLines(
						"A B C",
						"<lower>",
						"d e f",
						"</lower>",
						"G H I"));
	}

	/**
	 * @param name
	 *             The name of the formatter step
	 * @param function
	 *             The function used by the formatter step
	 * @return A FormatterStep which will never report that it is up-to-date, because
	 *         it is not equal to the serialized representation of itself.
	 */
	static <T extends FormatterFunc & Serializable> FormatterStep createNeverUpToDateSerializable(
			String name,
			T function) {
		Objects.requireNonNull(function, "function");
		return new NeverUpToDateSerializable(name, function);
	}

	static class NeverUpToDateSerializable<T extends FormatterFunc & Serializable> implements FormatterStep, Serializable {
		private final String name;
		private final T formatterFunc;

		private NeverUpToDateSerializable(String name, T formatterFunc) {
			this.name = name;
			this.formatterFunc = formatterFunc;
		}

		@Override
		public String getName() {
			return name;
		}

		@Nullable
		@Override
		public String format(String rawUnix, File file) throws Exception {
			return formatterFunc.apply(rawUnix, file);
		}

	}
}
