/*
 * Copyright 2022 DiffPlug
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
package com.diffplug.spotless.json.gson;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diffplug.spotless.json.JsonFormatterStepCommonTests;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class GsonStepTest extends JsonFormatterStepCommonTests {

	private static final String DEFAULT_VERSION = "2.8.9";

	@Test
	void handlesComplexNestedObject() throws Exception {
		doWithResource("cucumberJsonSampleGson");
	}

	@Test
	void handlesObjectWithNull() throws Exception {
		doWithResource("objectWithNullGson");
	}

	@Test
	void handlesInvalidJson() {
		assertThatThrownBy(() -> doWithResource("invalidJson"))
				.isInstanceOf(AssertionError.class)
				.hasMessage("Unable to format JSON")
				.hasRootCauseMessage("End of input at line 3 column 1 path $.a");
	}

	@Test
	void handlesNotJson() {
		assertThatThrownBy(() -> doWithResource("notJson"))
				.isInstanceOf(AssertionError.class)
				.hasMessage("Unable to format JSON")
				.hasNoCause();
	}

	@Test
	void handlesSortingWhenSortByKeyEnabled() throws Exception {
		FormatterStep step = GsonStep.create(INDENT, true, false, DEFAULT_VERSION, TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);
		stepHarness.testResource("json/sortByKeysBefore.json", "json/sortByKeysAfter.json");
	}

	@Test
	void doesNoSortingWhenSortByKeyDisabled() throws Exception {
		FormatterStep step = GsonStep.create(INDENT, false, false, DEFAULT_VERSION, TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("json/sortByKeysBefore.json", "json/sortByKeysAfterDisabled.json");
	}

	@Test
	void handlesHtmlEscapeWhenEnabled() throws Exception {
		FormatterStep step = GsonStep.create(INDENT, false, true, DEFAULT_VERSION, TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("json/escapeHtmlGsonBefore.json", "json/escapeHtmlGsonAfter.json");
	}

	@Test
	void writesRawHtmlWhenHtmlEscapeDisabled() throws Exception {
		FormatterStep step = GsonStep.create(INDENT, false, false, DEFAULT_VERSION, TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("json/escapeHtmlGsonBefore.json", "json/escapeHtmlGsonAfterDisabled.json");
	}

	@Test
	void handlesVersionIncompatibility() {
		FormatterStep step = GsonStep.create(INDENT, false, false, "1.7", TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);
		assertThatThrownBy(() -> stepHarness.testResource("json/cucumberJsonSampleGsonBefore.json", "json/cucumberJsonSampleGsonAfter.json"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("There was a problem interacting with Gson; maybe you set an incompatible version?");
	}

	@Override
	protected FormatterStep createFormatterStep(int indent, Provisioner provisioner) {
		return GsonStep.create(indent, false, false, DEFAULT_VERSION, provisioner);
	}
}
