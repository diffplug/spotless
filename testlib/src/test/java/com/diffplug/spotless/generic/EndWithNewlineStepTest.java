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
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;

class EndWithNewlineStepTest {
	@Test
	void behavior() throws Exception {
		StepHarness harness = StepHarness.forStep(EndWithNewlineStep.create());
		harness.test("", "\n");
		harness.test("\n\n\n\n", "\n");
		harness.test("line", "line\n");
		harness.test("line\nline\n\n\n\n", "line\nline\n");
		harness.testUnaffected("\n");
		harness.testUnaffected("line\n");
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
