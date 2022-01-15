/*
 * Copyright 2021-2022 DiffPlug
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

import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;

class DiktatStepTest extends ResourceHarness {
	@Test
	void behavior() {
		StepHarnessWithFile.forStep(this, DiktatStep.create(TestProvisioner.mavenCentral()))
				.testResource("src/main/kotlin/Main.kt", "kotlin/diktat/main.dirty", "kotlin/diktat/main.clean");
		StepHarnessWithFile.forStep(this, DiktatStep.create("1.0.1", TestProvisioner.mavenCentral()))
				.testResource("src/main/kotlin/Main.kt", "kotlin/diktat/main.dirty", "kotlin/diktat/main.clean");
	}
}
