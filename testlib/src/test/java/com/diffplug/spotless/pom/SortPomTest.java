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
package com.diffplug.spotless.pom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.diffplug.spotless.*;

public class SortPomTest extends ResourceHarness {
	@Test
	public void testSortPomWithDefaultConfig() {
		SortPomCfg cfg = new SortPomCfg();
		FormatterStep step = SortPomStep.create(cfg, TestProvisioner.mavenCentral());
		StepHarness.forStep(step).testResource("pom/pom_dirty.xml", "pom/pom_clean_default.xml");
	}

	@ParameterizedTest
	@ValueSource(strings = {"3.2.1", "3.3.0", "3.4.1", "4.0.0"})
	public void testSortPomWithVersion(String version) {
		SortPomCfg cfg = new SortPomCfg();
		cfg.version = version;
		FormatterStep step = SortPomStep.create(cfg, TestProvisioner.mavenCentral());
		StepHarness.forStep(step).testResource("pom/pom_dirty.xml", "pom/pom_clean_default.xml");
	}
}
