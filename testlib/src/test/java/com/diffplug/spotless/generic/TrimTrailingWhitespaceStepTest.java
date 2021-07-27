/*
 * Copyright 2016-2021 DiffPlug
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

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;

class TrimTrailingWhitespaceStepTest extends ResourceHarness {
	@Test
	void trimTrailingWhitespace() throws Exception {
		StepHarness step = StepHarness.forStep(TrimTrailingWhitespaceStep.create());
		step.testUnaffected("");
		step.testUnaffected("\n");
		step.testUnaffected("\n\n\n");
		step.testUnaffected("   preceding");

		step.test("trailing  ", "trailing");
		step.test("trailing  \n", "trailing\n");
		step.test("trailing\t", "trailing");
		step.test("trailing\t\n", "trailing\n");

		step.test("\t  trailing  ", "\t  trailing");
		step.test("\t  trailing  \n", "\t  trailing\n");
		step.test("\t  trailing\t", "\t  trailing");
		step.test("\t  trailing\t\n", "\t  trailing\n");

		step.testUnaffected("Line\nLine");
		step.test("Line  \nLine", "Line\nLine");
		step.test("Line\nLine  ", "Line\nLine");
		step.test("Line  \nLine  ", "Line\nLine");
		step.test("  Line  \nLine  ", "  Line\nLine");
		step.test("  Line  \n  Line  ", "  Line\n  Line");
	}

	@Test
	void equality() throws Exception {
		new SerializableEqualityTester() {
			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return EndWithNewlineStep.create();
			}
		}.testEquals();
	}
}
