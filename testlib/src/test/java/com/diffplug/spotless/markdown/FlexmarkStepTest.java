/*
 * Copyright 2016-2026 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

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
	void flexmarkOptionsRightMargin() {
		FlexmarkConfig config = new FlexmarkConfig();
		config.setExtensions(List.of("YamlFrontMatter"));
		config.setFormatterOptions(Map.of("RIGHT_MARGIN", "100"));
		StepHarness.forStep(FlexmarkStep.create(TestProvisioner.mavenCentral(), config))
				.testResource(
						"markdown/flexmark/FlexmarkOptionsUnformatted.md",
						"markdown/flexmark/FlexmarkOptionsFormatted.md");
	}

	@Test
	void flexmarkOptionsUnknownKeyFails() {
		FlexmarkConfig config = new FlexmarkConfig();
		config.setFormatterOptions(Map.of("NON_EXISTENT_OPTION", "value"));
		assertThatThrownBy(() -> StepHarness.forStep(FlexmarkStep.create(TestProvisioner.mavenCentral(), config))
				.test("text\n", "text\n"))
				.hasRootCauseInstanceOf(IllegalArgumentException.class)
				.hasRootCauseMessage(
						"Unknown flexmark formatter option: no field Formatter.NON_EXISTENT_OPTION."
								+ " See https://github.com/vsch/flexmark-java/wiki/Markdown-Formatter#options");
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
