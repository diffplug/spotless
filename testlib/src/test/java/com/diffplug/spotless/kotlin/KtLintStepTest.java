/*
 * Copyright 2016-2023 DiffPlug
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
package com.diffplug.spotless.kotlin;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;

class KtLintStepTest extends ResourceHarness {
	@Test
	void works0_48_0() {
		FormatterStep step = KtLintStep.create("0.48.0", TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic-old.clean")
				.testResourceExceptionMsg("kotlin/ktlint/unsolvable.dirty").isEqualTo("Error on line: 1, column: 1\n" +
						"rule: no-wildcard-imports\n" +
						"Wildcard import");
	}

	@Test
	void works0_48_1() {
		FormatterStep step = KtLintStep.create("0.48.1", TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic-old.clean")
				.testResourceExceptionMsg("kotlin/ktlint/unsolvable.dirty").isEqualTo("Error on line: 1, column: 1\n" +
						"rule: no-wildcard-imports\n" +
						"Wildcard import");
	}

	@Test
	void works0_49_0() {
		FormatterStep step = KtLintStep.create("0.49.0", TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic-old.clean")
				.testResourceExceptionMsg("kotlin/ktlint/unsolvable.dirty").isEqualTo("Error on line: 1, column: 1\n" +
						"rule: standard:no-wildcard-imports\n" +
						"Wildcard import");
	}

	@Test
	void works0_49_1() {
		FormatterStep step = KtLintStep.create("0.49.1", TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic-old.clean")
				.testResourceExceptionMsg("kotlin/ktlint/unsolvable.dirty").isEqualTo("Error on line: 1, column: 1\n" +
						"rule: standard:no-wildcard-imports\n" +
						"Wildcard import");
	}

	@Test
	void works0_50_0() {
		FormatterStep step = KtLintStep.create("0.50.0", TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic.clean")
				.testResourceExceptionMsg("kotlin/ktlint/unsolvable.dirty").isEqualTo("Error on line: 1, column: 1\n" +
						"rule: standard:no-wildcard-imports\n" +
						"Wildcard import");
	}

	@Test
	void works1_0_0() {
		FormatterStep step = KtLintStep.create("1.0.0", TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic.clean")
				.testResourceExceptionMsg("kotlin/ktlint/unsolvable.dirty").isEqualTo("Error on line: 1, column: 1\n" +
						"rule: standard:no-empty-file\n" +
						"File 'unsolvable.dirty' should not be empty");
	}

	@Test
	void behavior() {
		FormatterStep step = KtLintStep.create(TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic.clean")
				.testResourceExceptionMsg("kotlin/ktlint/unsolvable.dirty").isEqualTo("Error on line: 1, column: 1\n" +
						"rule: standard:no-empty-file\n" +
						"File 'unsolvable.dirty' should not be empty");
	}

	@Test
	void equality() {
		new SerializableEqualityTester() {
			String version = "0.48.0";

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "0.48.1";
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				String finalVersion = this.version;
				return KtLintStep.create(finalVersion, TestProvisioner.mavenCentral());
			}
		}.testEquals();
	}
}
