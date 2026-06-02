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

class AsciidocFormatterStepTest extends ResourceHarness {

	@Test
	void behavior() {
		try (StepHarness step = StepHarness.forStep(AsciidocFormatterStep.create(new AsciidocFormatterConfig()))) {
			step.testResource("asciidoc/asciidocBefore.adoc", "asciidoc/asciidocAfter.adoc");
		}
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			AsciidocFormatterConfig config = new AsciidocFormatterConfig();

			@Override
			protected void setupTest(API api) {
				// baseline — default config
				api.areDifferentThan();

				// each field change produces a distinct step
				config.setNormalizeSetextHeadings(!config.isNormalizeSetextHeadings());
				api.areDifferentThan();

				config.setCollapseConsecutiveBlankLines(!config.isCollapseConsecutiveBlankLines());
				api.areDifferentThan();

				config.setOneSentencePerLine(!config.isOneSentencePerLine());
				api.areDifferentThan();

				config.setNormalizeBlockDelimiters(!config.isNormalizeBlockDelimiters());
				api.areDifferentThan();

				config.setRemoveTrailingHeaderEqualsSign(!config.isRemoveTrailingHeaderEqualsSign());
				api.areDifferentThan();

				config.setTitleCase(!config.isTitleCase());
				api.areDifferentThan();

				config.setRemoveTrailingWhitespace(!config.isRemoveTrailingWhitespace());
				api.areDifferentThan();

				config.setNormalizeListBullets(!config.isNormalizeListBullets());
				api.areDifferentThan();

				config.setNormalizeOrderedListMarkers(!config.isNormalizeOrderedListMarkers());
				api.areDifferentThan();

				config.setEnsureHeadingBlankLines(!config.isEnsureHeadingBlankLines());
				api.areDifferentThan();

				config.setEnsureSourceDelimiters(!config.isEnsureSourceDelimiters());
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				AsciidocFormatterConfig snapshot = new AsciidocFormatterConfig();
				snapshot.setNormalizeSetextHeadings(config.isNormalizeSetextHeadings());
				snapshot.setCollapseConsecutiveBlankLines(config.isCollapseConsecutiveBlankLines());
				snapshot.setOneSentencePerLine(config.isOneSentencePerLine());
				snapshot.setNormalizeBlockDelimiters(config.isNormalizeBlockDelimiters());
				snapshot.setRemoveTrailingHeaderEqualsSign(config.isRemoveTrailingHeaderEqualsSign());
				snapshot.setTitleCase(config.isTitleCase());
				snapshot.setRemoveTrailingWhitespace(config.isRemoveTrailingWhitespace());
				snapshot.setNormalizeListBullets(config.isNormalizeListBullets());
				snapshot.setNormalizeOrderedListMarkers(config.isNormalizeOrderedListMarkers());
				snapshot.setEnsureHeadingBlankLines(config.isEnsureHeadingBlankLines());
				snapshot.setEnsureSourceDelimiters(config.isEnsureSourceDelimiters());
				return AsciidocFormatterStep.create(snapshot);
			}
		}.testEquals();
	}
}
