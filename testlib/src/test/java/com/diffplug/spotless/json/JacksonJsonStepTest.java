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

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

import java.util.Map;

import static com.diffplug.spotless.json.JacksonJsonStep.defaultVersion;

class JacksonJsonStepTest {
	@Test
	void canSetCustomIndentationLevel() {
		FormatterStep step = JacksonJsonStep.create(TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStep(step);

		String before = "json/singletonArrayBefore.json";
		String after = "json/singletonArrayAfter_Jackson.json";
		stepHarness.testResource(before, after);
	}

	@Test
	void canSetJsonParserFeature() {
		JacksonJsonConfig jacksonJsonConfig = new JacksonJsonConfig();
		jacksonJsonConfig.appendJsonParserFeatureToToggle(Map.of("ALLOW_COMMENTS", true));
		FormatterStep step = JacksonJsonStep.create(jacksonJsonConfig, defaultVersion(), TestProvisioner.mavenCentral());
		StepHarness stepHarness = StepHarness.forStepNoRoundtrip(step);

		String before = "json/objectContainingComments.json";
		String after = "json/objectContainingCommentsAfter.json";
		stepHarness.testResource(before, after);
	}
}
