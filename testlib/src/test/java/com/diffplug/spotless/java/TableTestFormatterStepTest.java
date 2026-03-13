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
	void equality() {
		new SerializableEqualityTester() {
			String version = VERSION;

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();

				// change the version, and it's different
				version = "1.0.0";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return TableTestFormatterStep.create(version, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
