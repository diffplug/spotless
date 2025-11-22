/*
 * Copyright 2023-2025 DiffPlug
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
package com.diffplug.spotless.extra.groovy;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.TestProvisioner;

public class GrEclipseFormatterStepSpecialCaseTest {
	/**
	 * https://github.com/diffplug/spotless/issues/1657
	 * <p>
	 * broken: ${parm == null ? "" : "<tag>$parm</tag>"}
	 *  works: ${parm == null ? "" : "<tag>parm</tag>"}
	 */
	@Test
	public void issue_1657() {
		assertThrows(RuntimeException.class, () -> StepHarness.forStep(GrEclipseFormatterStep.createBuilder(TestProvisioner.mavenCentral()).build())
				.testResourceUnaffected("groovy/greclipse/format/SomeClass.test"));
	}

	@Test
	public void issue_1657_fixed() {
		StepHarness.forStep(GrEclipseFormatterStep.createBuilder(TestProvisioner.mavenCentral()).build())
				.testResourceUnaffected("groovy/greclipse/format/SomeClass.fixed");
	}
}
