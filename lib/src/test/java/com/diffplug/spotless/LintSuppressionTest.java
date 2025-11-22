/*
 * Copyright 2024-2025 DiffPlug
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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;

import com.diffplug.spotless.generic.EndWithNewlineStep;

public class LintSuppressionTest {
	private LintState dummyLintState() {
		var lints = new ArrayList<Lint>();
		lints.add(Lint.atLine(2, "66", "Order 66"));
		var perStep = new ArrayList<List<Lint>>();
		perStep.add(lints);
		return new LintState(DirtyState.clean(), perStep);
	}

	private Formatter formatter() {
		return Formatter.builder()
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(UTF_8)
				.steps(List.of(EndWithNewlineStep.create()))
				.build();
	}

	@Test
	public void testMatchSingle() {
		var noSuppressions = dummyLintState().withRemovedSuppressions(formatter(), "file", List.of());
		assertThat(noSuppressions.isHasLints()).isTrue();
		removesLint(s -> s.setStep("blah")).isFalse();
		removesLint(s -> s.setStep("endWithNewline")).isTrue();
		removesLint(s -> s.setPath("blah")).isFalse();
		removesLint(s -> s.setPath("testFile")).isTrue();
		removesLint(s -> s.setShortCode("blah")).isFalse();
		removesLint(s -> s.setShortCode("66")).isTrue();
	}

	@Test
	public void testMatchDouble() {
		removesLint(s -> {
			s.setStep("endWithNewline");
			s.setShortCode("blah");
		}).isFalse();
		removesLint(s -> {
			s.setStep("blah");
			s.setShortCode("66");
		}).isFalse();
		removesLint(s -> {
			s.setStep("endWithNewline");
			s.setShortCode("66");
		}).isTrue();
	}

	private AbstractBooleanAssert<?> removesLint(Consumer<LintSuppression> suppression) {
		var s = new LintSuppression();
		suppression.accept(s);

		var ls = dummyLintState().withRemovedSuppressions(formatter(), "testFile", List.of(s));
		return assertThat(!ls.isHasLints());
	}
}
