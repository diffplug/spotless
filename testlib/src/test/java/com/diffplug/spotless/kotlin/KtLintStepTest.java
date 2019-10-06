/*
 * Copyright 2016 DiffPlug
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

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class KtLintStepTest extends ResourceHarness {
	@Test
	public void behavior() throws Exception {
		// Must use jcenter because `com.andreapivetta.kolor:kolor:0.0.2` isn't available on mavenCentral.
		// It is a dependency of ktlint.
		FormatterStep step = KtLintStep.create(TestProvisioner.jcenter());
		StepHarness.forStep(step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic.clean")
				.testException("kotlin/ktlint/unsolvable.dirty", assertion -> {
					assertion.isInstanceOf(AssertionError.class);
					assertion.hasMessage("Error on line: 1, column: 1\n" +
							"Wildcard import");
				});
	}

	@Test
	public void worksPinterest() throws Exception {
		// Must use jcenter because `com.andreapivetta.kolor:kolor:0.0.2` isn't available on mavenCentral.
		// It is a dependency of ktlint.
		FormatterStep step = KtLintStep.create("0.31.0", TestProvisioner.jcenter());
		StepHarness.forStep(step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic.clean")
				.testException("kotlin/ktlint/unsolvable.dirty", assertion -> {
					assertion.isInstanceOf(AssertionError.class);
					assertion.hasMessage("Error on line: 1, column: 1\n" +
							"Wildcard import");
				});
	}

	@Test
	public void worksPinterest() throws Exception {
		// Must use jcenter because `com.andreapivetta.kolor:kolor:0.0.2` isn't available on mavenCentral.
		// It is a dependency of ktlint.
		FormatterStep step = KtLintStep.create("0.32.0", TestProvisioner.jcenter());
		StepHarness.forStep(step)
				.testResource("kotlin/ktlint/basic.dirty", "kotlin/ktlint/basic.clean")
				.testException("kotlin/ktlint/unsolvable.dirty", assertion -> {
					assertion.isInstanceOf(AssertionError.class);
					assertion.hasMessage("Error on line: 1, column: 1\n" +
							"Wildcard import");
				});
	}

	@Test
	public void equality() throws Exception {
		new SerializableEqualityTester() {
			String version = "0.2.2";

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				version = "0.2.1";
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
