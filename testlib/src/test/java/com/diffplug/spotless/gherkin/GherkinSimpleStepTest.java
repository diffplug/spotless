/*
 * Copyright 2021-2023 DiffPlug
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
package com.diffplug.spotless.gherkin;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class GherkinSimpleStepTest {

	private static final String VERSION = GherkinSimpleStep.defaultVersion();
	private static final int INDENT = GherkinSimpleConfig.defaultIndentSpaces();

	private final FormatterStep step = GherkinSimpleStep.create(new GherkinSimpleConfig(INDENT), VERSION, TestProvisioner.mavenCentral());
	private final StepHarness stepHarness = StepHarness.forStep(step);

	@Test
	public void cannotProvidedNullProvisioner() {
		assertThatThrownBy(() -> GherkinSimpleStep.create(new GherkinSimpleConfig(INDENT), VERSION, null)).isInstanceOf(NullPointerException.class).hasMessage("provisioner cannot be null");
	}

	@Test
	public void handlesEmptyFeature() throws Exception {
		doWithResource(stepHarness, "empty");
	}

	@Test
	public void handlesComplexBackground() throws Exception {
		doWithResource(stepHarness, "complex_background");
	}

	@Test
	public void handlesDescriptions() throws Exception {
		doWithResource(stepHarness, "descriptions");
	}

	@Test
	public void handlesRuleWithTag() throws Exception {
		doWithResource(stepHarness, "rule_with_tag");
	}

	@Test
	public void handlesInvalidGherkin() {
		assertThatThrownBy(() -> doWithResource(stepHarness, "invalidGherkin"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("No gherkin document");
	}

	@Test
	public void handlesNotGherkin() {
		assertThatThrownBy(() -> doWithResource(stepHarness, "notGherkin"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("No gherkin document");
	}

	@Disabled("gherkin-utils does not allow custom indentation")
	@Test
	public void canSetCustomIndentationLevel() throws Exception {
		FormatterStep step = GherkinSimpleStep.create(new GherkinSimpleConfig(6), VERSION, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "gherkin/minimalBefore.feature";
		String after = "gherkin/minimalAfter6Spaces.feature";
		stepHarness.testResource(before, after);
	}

	@Disabled("gherkin-utils does not allow custom indentation")
	@Test
	public void canSetIndentationLevelTo0() throws Exception {
		FormatterStep step = GherkinSimpleStep.create(new GherkinSimpleConfig(0), VERSION, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "gherkin/minimalBefore.feature";
		String after = "gherkin/minimalAfter0Spaces.feature";
		stepHarness.testResource(before, after);
	}

	@Test
	public void equality() {
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
				return GherkinSimpleStep.create(new GherkinSimpleConfig(spaces), VERSION, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}

	private static void doWithResource(StepHarness stepHarness, String name) {
		String before = String.format("gherkin/%sBefore.feature", name);
		String after = String.format("gherkin/%sAfter.feature", name);
		stepHarness.testResource(before, after);
	}
}
