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

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.StepHarness;

class ForbidModuleImportsStepTest {
	@Test
	void detectsModuleImport() {
		FormatterStep step = ForbidModuleImportsStep.create();
		StepHarness.forStep(step)
				.expectLintsOf(StringPrinter.buildStringFromLines(
						"import module java.base;",
						"import java.util.List;"))
				.toBe("L1 forbidModuleImports(import module java.base;) Do not use module imports - replace with specific class imports as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void detectsIndentedModuleImport() {
		FormatterStep step = ForbidModuleImportsStep.create();
		StepHarness.forStep(step)
				.expectLintsOf(StringPrinter.buildStringFromLines(
						"  import module java.base;",
						"\timport module java.sql;"))
				.toBe("L1 forbidModuleImports(import module java.base;) Do not use module imports - replace with specific class imports as 'spotlessApply' cannot auto-fix this",
						"L2 forbidModuleImports(import module java.sql;) Do not use module imports - replace with specific class imports as 'spotlessApply' cannot auto-fix this");
	}
}
