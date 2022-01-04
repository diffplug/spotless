/*
 * Copyright 2020-2022 DiffPlug
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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.StepHarness;

class PipeStepPairTest {
	@Test
	void single() throws Exception {
		PipeStepPair pair = PipeStepPair.named("underTest").openClose("spotless:off", "spotless:on").buildPair();
		FormatterStep lowercase = FormatterStep.createNeverUpToDate("lowercase", str -> str.toLowerCase(Locale.ROOT));
		StepHarness harness = StepHarness.forSteps(pair.in(), lowercase, pair.out());
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
	void multiple() throws Exception {
		PipeStepPair pair = PipeStepPair.named("underTest").openClose("spotless:off", "spotless:on").buildPair();
		FormatterStep lowercase = FormatterStep.createNeverUpToDate("lowercase", str -> str.toLowerCase(Locale.ROOT));
		StepHarness harness = StepHarness.forSteps(pair.in(), lowercase, pair.out());
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
	void broken() throws Exception {
		PipeStepPair pair = PipeStepPair.named("underTest").openClose("spotless:off", "spotless:on").buildPair();
		FormatterStep uppercase = FormatterStep.createNeverUpToDate("uppercase", str -> str.toUpperCase(Locale.ROOT));
		StepHarness harness = StepHarness.forSteps(pair.in(), uppercase, pair.out());
		// this fails because uppercase turns spotless:off into SPOTLESS:OFF, etc
		harness.testException(StringPrinter.buildStringFromLines(
				"A B C",
				"spotless:off",
				"D E F",
				"spotless:on",
				"G H I"), exception -> {
					exception.hasMessage("An intermediate step removed a match of spotless:off spotless:on");
				});
	}

	@Test
	void andApply() throws Exception {
		FormatterStep lowercase = FormatterStep.createNeverUpToDate("lowercase", str -> str.toLowerCase(Locale.ROOT));
		FormatterStep lowercaseSometimes = PipeStepPair.named("lowercaseSometimes").openClose("<lower>", "</lower>")
				.buildStepWhichAppliesSubSteps(Paths.get(""), Arrays.asList(lowercase));
		StepHarness.forSteps(lowercaseSometimes).test(
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
}
