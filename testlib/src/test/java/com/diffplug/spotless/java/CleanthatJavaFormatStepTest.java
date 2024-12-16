/*
 * Copyright 2016-2024 DiffPlug
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
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;

class CleanthatJavaFormatStepTest extends ResourceHarness {

	@Test
	void testDefault() {
		FormatterStep step = CleanthatJavaStep.create(TestProvisioner.mavenCentral());
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, step)) {
			harness.testResource("java/cleanthat/MultipleMutators.dirty.test", "java/cleanthat/MultipleMutators.dirty.test");
		}
	}

	@Test
	void test11IncludeDraft() {
		FormatterStep step = CleanthatJavaStep.create("io.github.solven-eu.cleanthat:java",
				CleanthatJavaStep.defaultVersion(),
				"11",
				CleanthatJavaStep.defaultMutators(), CleanthatJavaStep.defaultExcludedMutators(), true, TestProvisioner.mavenCentral());
		try (StepHarnessWithFile harness = StepHarnessWithFile.forStep(this, step)) {
			harness.testResource("java/cleanthat/MultipleMutators.dirty.test", "java/cleanthat/MultipleMutators.clean.onlyOptionalIsPresent.test");
		}
	}

}
