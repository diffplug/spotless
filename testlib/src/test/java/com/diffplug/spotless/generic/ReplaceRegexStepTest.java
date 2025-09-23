/*
 * Copyright 2025 DiffPlug
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

import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.StepHarness;

class ReplaceRegexStepTest {
	@Test
	void formatter() throws Exception {
		FormatterStep step = ReplaceRegexStep.create("replace", "bad", "good");
		StepHarness.forStep(step)
				.test("bad bad", "good good");
	}

	@Test
	void lint() throws Exception {
		FormatterStep step = ReplaceRegexStep.lint("stayPositive", "(bad|awful)", "no negative words");
		StepHarness.forStep(step)
				.expectLintsOf(StringPrinter.buildStringFromLines(
						"bad",
						"oh boy",
						"x awful y",
						"yippeee"))
				.toBe("L1 stayPositive(bad) no negative words",
						"L3 stayPositive(awful) no negative words");
	}
}
