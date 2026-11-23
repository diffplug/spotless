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
package com.diffplug.spotless;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.generic.EndWithNewlineStep;
import com.diffplug.spotless.generic.TrimTrailingWhitespaceStep;

class LintStateTest {
	private static final Lint FORWARDED_A = Lint.atLine(1, "a", "forwarded a").withStepName("inner");
	private static final Lint OWN = Lint.atLine(2, "own", "own lint");
	private static final Lint FORWARDED_B = Lint.atLine(3, "b", "forwarded b").withStepName("inner");
	private static final Lint SECOND_STEP = Lint.atLine(4, "second", "second step lint");

	private Formatter formatter() {
		return Formatter.builder()
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(StandardCharsets.UTF_8)
				.steps(List.of(EndWithNewlineStep.create(), TrimTrailingWhitespaceStep.create()))
				.build();
	}

	private LintState lintState() {
		var perStep = new ArrayList<List<Lint>>();
		perStep.add(List.of(FORWARDED_A, OWN, FORWARDED_B));
		perStep.add(List.of(SECOND_STEP));
		return new LintState(DirtyState.clean(), perStep);
	}

	@Test
	void getLintsByStepGroupsByOriginatingStepInOrder() {
		var byStep = lintState().getLintsByStep(formatter());

		assertThat(byStep.keySet()).containsExactly("inner", "endWithNewline", "trimTrailingWhitespace");
		assertThat(byStep.get("inner")).containsExactly(FORWARDED_A, FORWARDED_B);
		assertThat(byStep.get("endWithNewline")).containsExactly(OWN);
		assertThat(byStep.get("trimTrailingWhitespace")).containsExactly(SECOND_STEP);
	}

	@Test
	void asStringUsesOriginatingStepName() {
		assertThat(lintState().asStringOneLine(new File("Test.java"), formatter())).isEqualTo(String.join("\n",
				"Test.java:L1 inner(a) forwarded a",
				"Test.java:L2 endWithNewline(own) own lint",
				"Test.java:L3 inner(b) forwarded b",
				"Test.java:L4 trimTrailingWhitespace(second) second step lint"));
	}
}
