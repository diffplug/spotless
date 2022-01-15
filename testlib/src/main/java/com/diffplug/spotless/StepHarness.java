/*
 * Copyright 2016-2022 DiffPlug
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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;

/** An api for testing a {@code FormatterStep} that doesn't depend on the File path. DO NOT ADD FILE SUPPORT TO THIS, use {@link StepHarnessWithFile} if you need that. */
public class StepHarness implements AutoCloseable {
	private final Formatter formatter;

	private StepHarness(Formatter formatter) {
		this.formatter = Objects.requireNonNull(formatter);
	}

	/** Creates a harness for testing steps which don't depend on the file. */
	public static StepHarness forStep(FormatterStep step) {
		return forSteps(step);
	}

	/** Creates a harness for testing steps which don't depend on the file. */
	public static StepHarness forSteps(FormatterStep... steps) {
		return forFormatter(Formatter.builder()
				.steps(Arrays.asList(steps))
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(StandardCharsets.UTF_8)
				.build());
	}

	/** Creates a harness for testing a formatter whose steps don't depend on the file. */
	public static StepHarness forFormatter(Formatter formatter) {
		return new StepHarness(formatter);
	}

	/** Asserts that the given element is transformed as expected, and that the result is idempotent. */
	public StepHarness test(String before, String after) {
		String actual = formatter.compute(LineEnding.toUnix(before), new File(""));
		assertEquals(after, actual, "Step application failed");
		return testUnaffected(after);
	}

	/** Asserts that the given element is idempotent w.r.t the step under test. */
	public StepHarness testUnaffected(String idempotentElement) {
		String actual = formatter.compute(LineEnding.toUnix(idempotentElement), new File(""));
		assertEquals(idempotentElement, actual, "Step is not idempotent");
		return this;
	}

	/** Asserts that the given elements in  the resources directory are transformed as expected. */
	public StepHarness testResource(String resourceBefore, String resourceAfter) {
		String before = ResourceHarness.getTestResource(resourceBefore);
		String after = ResourceHarness.getTestResource(resourceAfter);
		return test(before, after);
	}

	/** Asserts that the given elements in the resources directory are transformed as expected. */
	public StepHarness testResourceUnaffected(String resourceIdempotent) {
		String idempotentElement = ResourceHarness.getTestResource(resourceIdempotent);
		return testUnaffected(idempotentElement);
	}

	public AbstractStringAssert<?> testResourceExceptionMsg(String resourceBefore) {
		return testExceptionMsg(ResourceHarness.getTestResource(resourceBefore));
	}

	public AbstractStringAssert<?> testExceptionMsg(String before) {
		List<Lint> lints = formatter.lint(LineEnding.toUnix(before), FormatterStepImpl.SENTINEL);
		if (lints.size() == 0) {
			throw new AssertionError("No exception was thrown");
		} else if (lints.size() >= 2) {
			throw new AssertionError("Expected one lint, had " + lints.size());
		} else {
			return Assertions.assertThat(lints.get(0).getMsg());
		}
	}

	public StepHarness assertZeroLints(String before) {
		List<Lint> lints = formatter.lint(LineEnding.toUnix(before), FormatterStepImpl.SENTINEL);
		if (lints.size() == 0) {
			return this;
		} else {
			throw new AssertionError("Expected no lints, had " + lints);
		}
	}

	@Override
	public void close() {
		formatter.close();
	}
}
