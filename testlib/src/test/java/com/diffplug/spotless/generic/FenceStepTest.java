/*
 * Copyright 2020-2025 DiffPlug
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
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.StepHarnessWithFile;

class FenceStepTest extends ResourceHarness {
	@Test
	void single() {
		FormatterStep fence = FenceStep.named("fence").openClose("spotless:off", "spotless:on")
				.preserveWithin(Arrays.asList(ToCaseStep.lower()));
		StepHarness harness = StepHarness.forStep(fence);
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
				.preserveWithin(Arrays.asList(ToCaseStep.lower()));
		StepHarness harness = StepHarness.forStep(fence);
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
				.preserveWithin(Arrays.asList(ReplaceStep.create("replace", "spotless:on", "REMOVED")));
		// this fails because uppercase turns spotless:off into SPOTLESS:OFF, etc
		StepHarnessWithFile.forStep(this, fence).expectLintsOfFileAndContent("README.md", StringPrinter.buildStringFromLines("A B C",
				"spotless:off",
				"D E F",
				"spotless:on",
				"G H I")).toBe("L1-6 fence(fenceRemoved) An intermediate step removed a match of spotless:off spotless:on");
	}

	@Test
	void andApply() {
		FormatterStep fence = FenceStep.named("fence").openClose("<lower>", "</lower>")
				.applyWithin(Arrays.asList(ToCaseStep.lower()));
		StepHarness.forStep(fence).test(
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

	static class ToCaseStep implements FormatterStep {
		static ToCaseStep upper() {
			return new ToCaseStep(true);
		}

		static ToCaseStep lower() {
			return new ToCaseStep(false);
		}

		private final boolean uppercase;

		ToCaseStep(boolean uppercase) {
			this.uppercase = uppercase;
		}

		@Override
		public String getName() {
			return uppercase ? "uppercase" : "lowercase";
		}

		@org.jetbrains.annotations.Nullable @Override
		public String format(String rawUnix, File file) throws Exception {
			return uppercase ? rawUnix.toUpperCase() : rawUnix.toLowerCase();
		}

		@Override
		public void close() throws Exception {

		}

		@Override
		public int hashCode() {
			return getName().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ToCaseStep tcs && getName().equals(tcs.getName());
		}
	}
}
