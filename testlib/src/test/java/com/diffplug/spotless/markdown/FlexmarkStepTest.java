/*
 * Copyright 2016-2025 DiffPlug
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
package com.diffplug.spotless.markdown;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class FlexmarkStepTest {
	private static final String OLDEST_SUPPORTED = "0.62.2";

	@Test
	void behaviorOldest() {
		FlexmarkConfig config = new FlexmarkConfig();
		config.setExtensions(List.of("YamlFrontMatter"));
		StepHarness.forStep(FlexmarkStep.create(OLDEST_SUPPORTED, TestProvisioner.mavenCentral(), config))
				.testResource(
						"markdown/flexmark/FlexmarkUnformatted.md",
						"markdown/flexmark/FlexmarkFormatted.md");
	}

	@Test
	void behaviorLatest() {
		FlexmarkConfig config = new FlexmarkConfig();
		config.setExtensions(List.of("YamlFrontMatter"));
		StepHarness.forStep(FlexmarkStep.create(TestProvisioner.mavenCentral(), config))
				.testResource(
						"markdown/flexmark/FlexmarkUnformatted.md",
						"markdown/flexmark/FlexmarkFormatted.md");
	}
}
