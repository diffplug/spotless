/*
 * Copyright 2020-2021 DiffPlug
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
package com.diffplug.spotless.python;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.tag.BlackTest;

@BlackTest
class BlackStepTest {
	@Test
	void test() throws Exception {
		StepHarness.forStep(BlackStep.withVersion(BlackStep.defaultVersion()).create())
				.testResource("python/black/black.dirty", "python/black/black.clean")
				.close();
	}
}
