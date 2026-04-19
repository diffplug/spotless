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
package com.diffplug.spotless.java;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;

class TableTestFormatterStepTest extends ResourceHarness {

	private static final String VERSION = "1.1.1";

	@Test
	void behavior() {
		FormatterStep step = TableTestFormatterStep.create(VERSION, TestProvisioner.mavenCentral());
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, step)) {
			harness.testResource("CalculatorTest.java", "java/tableTestFormatter/JavaCodeUnformatted.test", "java/tableTestFormatter/JavaCodeFormatted.test");
		}
	}

	@Test
	void behaviorTableFile() {
		FormatterStep step = TableTestFormatterStep.create(VERSION, TestProvisioner.mavenCentral());
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, step)) {
			harness.testResource("example.table", "tableTest/tableFileUnformatted.test", "tableTest/tableFileFormatted.test");
		}
	}

	@Test
	void editorConfigChangesAreHonored() {
		// Verifies that a new FormatterStep picks up .editorconfig changes rather than
		// returning stale results from a shared cache.
		//
		// Two steps with the same version share a classloader (via SpotlessCache's
		// serialization-based key). If EditorConfigProvider were stored as a static field,
		// its permanent ec4j cache would survive across FormatterFunc instances, and the
		// second step would still see the indent_size=4 result cached by the first step.
		// Making EditorConfigProvider an instance field ensures each FormatterFunc gets a
		// fresh provider with an empty cache.

		// Step 1: .editorconfig with indent_size=4
		setFile(".editorconfig").toContent("[*.java]\nindent_size = 4\n");
		FormatterStep step1 = TableTestFormatterStep.create(VERSION, TestProvisioner.mavenCentral());
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, step1)) {
			harness.testResource("CalculatorTest.java",
					"java/tableTestFormatter/JavaCodeUnformatted.test",
					"java/tableTestFormatter/JavaCodeFormatted.test");
		}

		// Step 2: change .editorconfig to indent_size=2, create a new step
		setFile(".editorconfig").toContent("[*.java]\nindent_size = 2\n");
		FormatterStep step2 = TableTestFormatterStep.create(VERSION, TestProvisioner.mavenCentral());
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, step2)) {
			harness.testResource("CalculatorTest.java",
					"java/tableTestFormatter/JavaCodeUnformatted.test",
					"java/tableTestFormatter/JavaCodeFormattedWith2SpaceIndent.test");
		}
	}

	@Test
	void behaviorWithConfiguredFallback() {
		// When no .editorconfig is present, the configured indentSize is used as the fallback
		// instead of the built-in default (Config.SPACES_4 = 4 spaces).
		// Configuring indentSize=2 should produce the same output as when editorconfig sets
		// indent_size=2.
		FormatterStep step = TableTestFormatterStep.create(VERSION, TestProvisioner.mavenCentral(), "space", 2);
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, step)) {
			harness.testResource("CalculatorTest.java",
					"java/tableTestFormatter/JavaCodeUnformatted.test",
					"java/tableTestFormatter/JavaCodeFormattedWith2SpaceIndent.test");
		}
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			String version = VERSION;
			String indentStyle = TableTestFormatterStep.DEFAULT_INDENT_STYLE;
			int indentSize = TableTestFormatterStep.DEFAULT_INDENT_SIZE;

			@Override
			protected void setupTest(API api) {
				// same version + defaults == same
				api.areDifferentThan();

				// change the version
				version = "1.0.0";
				api.areDifferentThan();

				// restore version, change indent size
				version = VERSION;
				indentSize = 2;
				api.areDifferentThan();

				// change indent style
				indentSize = TableTestFormatterStep.DEFAULT_INDENT_SIZE;
				indentStyle = "tab";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return TableTestFormatterStep.create(version, TestProvisioner.mavenCentral(), indentStyle, indentSize);
			}
		}.testEquals();
	}
}
