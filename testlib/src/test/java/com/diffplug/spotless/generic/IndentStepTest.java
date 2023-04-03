/*
 * Copyright 2016-2023 DiffPlug
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

class IndentStepTest {
	@Test
	void tabToTab() {
		FormatterStep indent = IndentStep.Type.TAB.create(4);
		StepHarness.forStep(indent).testResource("indent/IndentedWithTab.test", "indent/IndentedWithTab.test");
	}

	@Test
	void spaceToSpace() {
		FormatterStep indent = IndentStep.Type.SPACE.create(4);
		StepHarness.forStep(indent).testResource("indent/IndentedWithSpace.test", "indent/IndentedWithSpace.test");
	}

	@Test
	void spaceToTab() {
		FormatterStep indent = IndentStep.Type.TAB.create(4);
		StepHarness.forStep(indent).testResource("indent/IndentedWithSpace.test", "indent/IndentedWithTab.test");
	}

	@Test
	void tabToSpace() {
		FormatterStep indent = IndentStep.Type.SPACE.create(4);
		StepHarness.forStep(indent).testResource("indent/IndentedWithTab.test", "indent/IndentedWithSpace.test");
	}

	@Test
	void doesntClipNewlines() {
		FormatterStep indent = IndentStep.Type.SPACE.create(4);
		var blankNewlines = "\n\n\n\n";
		StepHarness.forStep(indent).testUnaffected(blankNewlines);
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			IndentStep.Type type = IndentStep.Type.SPACE;
			int numSpacesPerTab = 2;

			@Override
			protected void setupTest(API api) {
				api.areDifferentThan();

				numSpacesPerTab = 4;
				api.areDifferentThan();

				type = IndentStep.Type.TAB;
				api.areDifferentThan();

				numSpacesPerTab = 2;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return type.create(numSpacesPerTab);
			}
		}.testEquals();
	}

	@Test
	void allowLeadingSpaceIfMultiLineComment() throws Exception {
		StepHarness.forStep(IndentStep.Type.TAB.create(4))
				.testUnaffected("* test")
				.testUnaffected(" * test")
				.testUnaffected("\t* test")
				.test("    * test", "\t* test");
	}
}
