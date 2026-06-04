/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.asciidoc;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

class AdocfmtStepTest extends ResourceHarness {
	@Test
	void behavior() {
		FormatterStep step = AdocfmtStep.create(TestProvisioner.mavenCentral());
		StepHarness.forStep(step).testResource("asciidoc/adocfmt/dirty.adoc", "asciidoc/adocfmt/clean_minimal.adoc");
	}

	@Test
	void complexBehavior() {
		AdocfmtConfig config = new AdocfmtConfig();
		config.normalizeListBullets = true;
		config.normalizeOrderedListMarkers = true;
		config.ensureSourceDelimiters = true;
		FormatterStep step = AdocfmtStep.create(AdocfmtStep.defaultVersion(), TestProvisioner.mavenCentral(), config);
		StepHarness.forStep(step).testResource("asciidoc/adocfmt/dirty.adoc", "asciidoc/adocfmt/clean_complex.adoc");
	}

	@Test
	void customConfigBehavior() {
		AdocfmtConfig config = new AdocfmtConfig();
		config.ensureHeadingBlankLines = false;

		FormatterStep step = AdocfmtStep.create(AdocfmtStep.defaultVersion(), TestProvisioner.mavenCentral(), config);

		StepHarness.forStep(step).testResource("asciidoc/adocfmt/dirty.adoc", "asciidoc/adocfmt/clean_custom.adoc");
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			// fields drive create(); never mutate a config object after passing it to create()
			boolean titleCase = false;
			boolean normalizeListBullets = false;

			@Override
			protected void setupTest(API api) {
				// default config == same
				api.areDifferentThan();
				// change one config option -> different
				titleCase = true;
				api.areDifferentThan();
				// change another config option -> different
				titleCase = false;
				normalizeListBullets = true;
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				AdocfmtConfig config = new AdocfmtConfig();
				config.titleCase = titleCase;
				config.normalizeListBullets = normalizeListBullets;
				return AdocfmtStep.create(AdocfmtStep.defaultVersion(), TestProvisioner.mavenCentral(), config);
			}
		}.testEquals();
	}
}
