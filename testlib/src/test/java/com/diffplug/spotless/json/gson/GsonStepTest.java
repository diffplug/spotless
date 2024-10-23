/*
 * Copyright 2022-2024 DiffPlug
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

import static com.diffplug.spotless.json.gson.GsonStep.DEFAULT_VERSION;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Provisioner;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.json.JsonFormatterStepCommonTests;

public class GsonStepTest extends JsonFormatterStepCommonTests {

	@Test
	void handlesComplexNestedObject() {
		doWithResource("cucumberJsonSampleGson");
	}

	@Test
	void handlesObjectWithNull() {
		doWithResource("objectWithNullGson");
	}

	@Test
	void handlesInvalidJson() {
		getStepHarness().expectLintsOfResource("json/invalidJsonBefore.json").toBe("L3 gson(com.google.gson.JsonSyntaxException) java.io.EOFException: End of input at line 3 column 1 path $.a (...)");
	}

	@Test
	void handlesNotJson() {
		getStepHarness().expectLintsOfResource("json/notJsonBefore.json").toBe("LINE_UNDEFINED gson(java.lang.IllegalArgumentException) Unable to parse JSON (...)");
	}

	@Test
	void handlesSortingWhenSortByKeyEnabled() {
		FormatterStep step = GsonStep.create(new GsonConfig(true, false, INDENT, DEFAULT_VERSION), TestProvisioner.mavenCentral());
		StepHarness.forStep(step).testResource("json/sortByKeysBefore.json", "json/sortByKeysAfter.json");
	}

	@Test
	void doesNoSortingWhenSortByKeyDisabled() {
		FormatterStep step = GsonStep.create(new GsonConfig(false, false, INDENT, DEFAULT_VERSION), TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("json/sortByKeysBefore.json", "json/sortByKeysAfterDisabled.json");
	}

	@Test
	void handlesHtmlEscapeWhenEnabled() {
		FormatterStep step = GsonStep.create(new GsonConfig(false, true, INDENT, DEFAULT_VERSION), TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("json/escapeHtmlGsonBefore.json", "json/escapeHtmlGsonAfter.json");
	}

	@Test
	void writesRawHtmlWhenHtmlEscapeDisabled() {
		FormatterStep step = GsonStep.create(new GsonConfig(false, false, INDENT, DEFAULT_VERSION), TestProvisioner.mavenCentral());
		StepHarness.forStep(step)
				.testResource("json/escapeHtmlGsonBefore.json", "json/escapeHtmlGsonAfterDisabled.json");
	}

	@Test
	void handlesVersionIncompatibility() {
		StepHarness.forStep(GsonStep.create(new GsonConfig(false, false, INDENT, "1.7"), TestProvisioner.mavenCentral()))
				.expectLintsOf("").toBe("LINE_UNDEFINED gson(java.lang.IllegalStateException) There was a problem interacting with Gson; maybe you set an incompatible version? (...)");
	}

	@Override
	protected FormatterStep createFormatterStep(int indent, Provisioner provisioner) {
		return GsonStep.create(new GsonConfig(false, false, indent, DEFAULT_VERSION), provisioner);
	}
}
