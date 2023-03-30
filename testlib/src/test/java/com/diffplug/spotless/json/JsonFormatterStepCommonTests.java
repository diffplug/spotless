/*
 * Copyright 2022-2023 DiffPlug
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

import com.diffplug.spotless.*;

public abstract class JsonFormatterStepCommonTests {

	protected static final int INDENT = 4;

	@Test
	void cannotProvideNullProvisioner() {
		assertThatThrownBy(() -> createFormatterStep(INDENT, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessage("provisioner cannot be null");
	}

	@Test
	void handlesNestedObject() throws Exception {
		doWithResource("nestedObject");
	}

	@Test
	void handlesSingletonArray() throws Exception {
		doWithResource("singletonArray");
	}

	@Test
	void handlesEmptyFile() throws Exception {
		doWithResource("empty");
	}

	@Test
	void canSetCustomIndentationLevel() throws Exception {
		FormatterStep step = createFormatterStep(6, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		var before = "json/singletonArrayBefore.json";
		var after = "json/singletonArrayAfter6Spaces.json";
		stepHarness.testResource(before, after);
	}

	@Test
	void canSetIndentationLevelTo0() throws Exception {
		FormatterStep step = createFormatterStep(0, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		var before = "json/singletonArrayBefore.json";
		var after = "json/singletonArrayAfter0Spaces.json";
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
				return createFormatterStep(spaces, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}

	protected abstract FormatterStep createFormatterStep(int indent, Provisioner provisioner);

	protected StepHarness getStepHarness() {
		return StepHarness.forStep(createFormatterStep(INDENT, TestProvisioner.mavenCentral()));
	}

	protected void doWithResource(String name) {
		var before = String.format("json/%sBefore.json", name);
		var after = String.format("json/%sAfter.json", name);
		getStepHarness().testResource(before, after);
	}
}
