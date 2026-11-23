/*
 * Copyright 2024-2026 DiffPlug
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

import java.nio.charset.StandardCharsets;
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
				.encoding(StandardCharsets.UTF_8)
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

	@Test
	public void testMatchForwardedLintByOriginatingOrWrappingStep() {
		removesForwardedLint(s -> s.setStep("forbidWildcardImports")).isTrue();
		removesForwardedLint(s -> s.setStep("endWithNewline")).isTrue();
		removesForwardedLint(s -> s.setStep("blah")).isFalse();
		removesForwardedLint(s -> {
			s.setStep("forbidWildcardImports");
			s.setShortCode("blah");
		}).isFalse();
	}

	private AbstractBooleanAssert<?> removesLint(Consumer<LintSuppression> suppression) {
		return removesLint(dummyLintState(), suppression);
	}

	private AbstractBooleanAssert<?> removesForwardedLint(Consumer<LintSuppression> suppression) {
		var lints = new ArrayList<Lint>();
		lints.add(Lint.atLine(2, "66", "Order 66").withStepName("forbidWildcardImports"));
		var perStep = new ArrayList<List<Lint>>();
		perStep.add(lints);
		return removesLint(new LintState(DirtyState.clean(), perStep), suppression);
	}

	private AbstractBooleanAssert<?> removesLint(LintState lintState, Consumer<LintSuppression> suppression) {
		var s = new LintSuppression();
		suppression.accept(s);

		var ls = lintState.withRemovedSuppressions(formatter(), "testFile", List.of(s));
		return assertThat(!ls.isHasLints());
	}
}
