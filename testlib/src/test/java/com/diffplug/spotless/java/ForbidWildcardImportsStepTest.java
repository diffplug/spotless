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

class ForbidWildcardImportsStepTest {
	@Test
	void detectsPlainAndStaticWildcards() {
		FormatterStep step = ForbidWildcardImportsStep.create();
		StepHarness.forStep(step)
				.expectLintsOf(StringPrinter.buildStringFromLines(
						"import java.util.*;",
						"import static java.util.Collections.*;",
						"import java.util.List;"))
				.toBe("L1 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this",
						"L2 forbidWildcardImports(import static java.util.Collections.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}

	@Test
	void detectsIndentedWildcardImport() {
		FormatterStep step = ForbidWildcardImportsStep.create();
		StepHarness.forStep(step)
				.expectLintsOf(StringPrinter.buildStringFromLines(
						"  import java.util.*;",
						"\timport static java.util.Collections.*;"))
				.toBe("L1 forbidWildcardImports(import java.util.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this",
						"L2 forbidWildcardImports(import static java.util.Collections.*;) Do not use wildcard imports (e.g. java.util.*) - replace with specific class imports (e.g. java.util.List) as 'spotlessApply' cannot auto-fix this");
	}
}
