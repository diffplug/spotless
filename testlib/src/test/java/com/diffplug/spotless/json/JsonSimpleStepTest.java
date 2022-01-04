/*
 * Copyright 2021-2022 DiffPlug
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
package com.diffplug.spotless.json;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class JsonSimpleStepTest {

	private static final int INDENT = 4;

	private final FormatterStep step = JsonSimpleStep.create(INDENT, TestProvisioner.mavenCentral());
	private final StepHarness stepHarness = StepHarness.forStep(step);

	@Test
	void cannotProvidedNullProvisioner() {
		assertThatThrownBy(() -> JsonSimpleStep.create(INDENT, null)).isInstanceOf(NullPointerException.class).hasMessage("provisioner cannot be null");
	}

	@Test
	void handlesSingletonObject() throws Exception {
		doWithResource(stepHarness, "singletonObject");
	}

	@Test
	void handlesSingletonObjectWithArray() throws Exception {
		doWithResource(stepHarness, "singletonObjectWithArray");
	}

	@Test
	void handlesNestedObject() throws Exception {
		doWithResource(stepHarness, "nestedObject");
	}

	@Test
	void handlesSingletonArray() throws Exception {
		doWithResource(stepHarness, "singletonArray");
	}

	@Test
	void handlesEmptyFile() throws Exception {
		doWithResource(stepHarness, "empty");
	}

	@Test
	void handlesComplexNestedObject() throws Exception {
		doWithResource(stepHarness, "cucumberJsonSample");
	}

	@Test
	void handlesObjectWithNull() throws Exception {
		doWithResource(stepHarness, "objectWithNull");
	}

	@Test
	void handlesInvalidJson() {
		assertThatThrownBy(() -> doWithResource(stepHarness, "invalidJson"))
				.isInstanceOf(AssertionError.class)
				.hasMessage("Unable to format JSON")
				.hasRootCauseMessage("Expected a ',' or '}' at 9 [character 0 line 3]");
	}

	@Test
	void handlesNotJson() {
		assertThatThrownBy(() -> doWithResource(stepHarness, "notJson"))
				.isInstanceOf(AssertionError.class)
				.hasMessage("Unable to determine JSON type, expected a '{' or '[' but found '#'")
				.hasNoCause();
	}

	@Test
	void canSetCustomIndentationLevel() throws Exception {
		FormatterStep step = JsonSimpleStep.create(6, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "json/singletonArrayBefore.json";
		String after = "json/singletonArrayAfter6Spaces.json";
		stepHarness.testResource(before, after);
	}

	@Test
	void canSetIndentationLevelTo0() throws Exception {
		FormatterStep step = JsonSimpleStep.create(0, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "json/singletonArrayBefore.json";
		String after = "json/singletonArrayAfter0Spaces.json";
		stepHarness.testResource(before, after);
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			int spaces = 0;

			@Override
			protected void setupTest(API api) {
				// no changes, are the same
				api.areDifferentThan();

				// with different spacing
				spaces = 1;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return JsonSimpleStep.create(spaces, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}

	private static void doWithResource(StepHarness stepHarness, String name) throws Exception {
		String before = String.format("json/%sBefore.json", name);
		String after = String.format("json/%sAfter.json", name);
		stepHarness.testResource(before, after);
	}
}
