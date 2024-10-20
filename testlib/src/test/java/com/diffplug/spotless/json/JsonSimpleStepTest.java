/*
 * Copyright 2021-2024 DiffPlug
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
	void handlesSingletonObject() {
		doWithResource(stepHarness, "singletonObject");
	}

	@Test
	void handlesSingletonObjectWithArray() {
		doWithResource(stepHarness, "singletonObjectWithArray");
	}

	@Test
	void handlesNestedObject() {
		doWithResource(stepHarness, "nestedObject");
	}

	@Test
	void handlesSingletonArray() {
		doWithResource(stepHarness, "singletonArray");
	}

	@Test
	void handlesEmptyFile() {
		doWithResource(stepHarness, "empty");
	}

	@Test
	void handlesComplexNestedObject() {
		doWithResource(stepHarness, "cucumberJsonSample");
	}

	@Test
	void handlesObjectWithNull() {
		doWithResource(stepHarness, "objectWithNull");
	}

	@Test
	void handlesInvalidJson() {
		stepHarness.expectLintsOfResource("json/invalidJsonBefore.json").toBe("L3 jsonSimple(java.lang.IllegalArgumentException) Unable to format JSON",
				"\tat com.diffplug.spotless.json.JsonSimpleStep$State.format(JsonSimpleStep.java:109)",
				"\tat com.diffplug.spotless.json.JsonSimpleStep$State.lambda$toFormatter$0(JsonSimpleStep.java:94)",
				"\tat com.diffplug.spotless.FormatterFunc.apply(FormatterFunc.java:33)",
				"\tat com.diffplug.spotless.FormatterStepEqualityOnStateSerialization.format(FormatterStepEqualityOnStateSerialization.java:49)",
				"\tat com.diffplug.spotless.LintState.of(LintState.java:141)",
				"\tat com.diffplug.spotless.StepHarness.expectLintsOf(StepHarness.java:96)",
				"\tat com.diffplug.spotless.StepHarness.expectLintsOfResource(StepHarness.java:92)",
				"\tat com.diffplug.spotless.json.JsonSimpleStepTest.handlesInvalidJson(JsonSimpleStepTest.java:76)",
				"\tat java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)",
				"(... and more)");
	}

	@Test
	void handlesNotJson() {
		stepHarness.expectLintsOfResource("json/notJsonBefore.json")
				.toBe("LINE_UNDEFINED jsonSimple(java.lang.IllegalArgumentException) Unable to determine JSON type, expected a '{' or '[' but found '#'",
						"\tat com.diffplug.spotless.json.JsonSimpleStep$State.lambda$toFormatter$0(JsonSimpleStep.java:100)",
						"\tat com.diffplug.spotless.FormatterFunc.apply(FormatterFunc.java:33)",
						"\tat com.diffplug.spotless.FormatterStepEqualityOnStateSerialization.format(FormatterStepEqualityOnStateSerialization.java:49)",
						"\tat com.diffplug.spotless.LintState.of(LintState.java:141)",
						"\tat com.diffplug.spotless.StepHarness.expectLintsOf(StepHarness.java:96)",
						"\tat com.diffplug.spotless.StepHarness.expectLintsOfResource(StepHarness.java:92)",
						"\tat com.diffplug.spotless.json.JsonSimpleStepTest.handlesNotJson(JsonSimpleStepTest.java:91)",
						"\tat java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)",
						"\tat java.base/java.lang.reflect.Method.invoke(Method.java:580)",
						"(... and more)");
	}

	@Test
	void canSetCustomIndentationLevel() {
		FormatterStep step = JsonSimpleStep.create(6, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "json/singletonArrayBefore.json";
		String after = "json/singletonArrayAfter6Spaces.json";
		stepHarness.testResource(before, after);
	}

	@Test
	void canSetIndentationLevelTo0() {
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

	private static void doWithResource(StepHarness stepHarness, String name) {
		String before = String.format("json/%sBefore.json", name);
		String after = String.format("json/%sAfter.json", name);
		stepHarness.testResource(before, after);
	}
}
