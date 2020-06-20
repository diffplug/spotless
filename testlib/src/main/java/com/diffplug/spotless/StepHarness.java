/*
 * Copyright 2016-2020 DiffPlug
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

import java.io.File;
import java.util.Objects;
import java.util.function.Consumer;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.junit.Assert;

/**
 * An api for adding test cases.
 */
public class StepHarness implements AutoCloseable {
	private final FormatterFunc formatter;

	/**
	 * Creates a StepHarness around the given FormatterFunc.
	 */
	public StepHarness(FormatterFunc formatter) {
		this.formatter = Objects.requireNonNull(formatter);
	}

	/**
	 * Creates a harness for testing steps which do optionally depend on the file.
	 */
	public static StepHarness forStep(FormatterStep step) {
		// We don't care if an individual FormatterStep is misbehaving on line-endings, because
		// Formatter fixes that.  No reason to care in tests either.  It's likely to pop up when
		// running tests on Windows from time-to-time
		return new StepHarness(FormatterFunc.Closeable.of(
				() -> {
					if (step instanceof FormatterStepImpl.Standard) {
						((FormatterStepImpl.Standard<?>) step).cleanupFormatterFunc();
					}
				},
				new FormatterFunc() {
					@Override
					public String apply(String input) throws Exception {
						return LineEnding.toUnix(step.format(input, new File("")));
					}

					@Override
					public String apply(String input, File source) throws Exception {
						return LineEnding.toUnix(step.format(input, source));
					}
				}));
	}

	/**
	 * Creates a harness for testing a formatter whose steps don't depend on the file.
	 */
	public static StepHarness forFormatter(Formatter formatter) {
		return new StepHarness(FormatterFunc.Closeable.of(
				formatter::close,
				input -> formatter.compute(input, new File(""))));
	}

	/**
	 * Asserts that the given element is transformed as expected, and that the result is idempotent.
	 */
	public StepHarness test(String before, String after) throws Exception {
		return test("", before, "", after);
	}

	/**
	 * Asserts that the given resource containing element is transformed as expected, and that the result is idempotent.
	 */
	public StepHarness test(String resourceBefore, String before, String resourceAfter, String after) throws Exception {
		String actual = formatter.apply(before, new File(resourceBefore));
		Assert.assertEquals("Step application failed", after, actual);
		return testUnaffected(resourceAfter, after);

	}

	/**
	 * Asserts that the given element is idempotent w.r.t the step under test.
	 */
	public StepHarness testUnaffected(String idempotentElement) throws Exception {
		return testUnaffected("", idempotentElement);
	}

	/**
	 * Asserts that the given resource containing element is idempotent w.r.t the step under test.
	 */
	public StepHarness testUnaffected(String resourceIdempotent, String idempotentElement) throws Exception {
		String actual = formatter.apply(idempotentElement, new File(resourceIdempotent));
		Assert.assertEquals("Step is not idempotent", idempotentElement, actual);
		return this;
	}

	/**
	 * Asserts that the given elements in  the resources directory are transformed as expected.
	 */
	public StepHarness testResource(String resourceBefore, String resourceAfter) throws Exception {
		String before = ResourceHarness.getTestResource(resourceBefore);
		String after = ResourceHarness.getTestResource(resourceAfter);
		return test(resourceBefore, before, resourceAfter, after);
	}

	/**
	 * Asserts that the given elements in the resources directory are transformed as expected.
	 */
	public StepHarness testResourceUnaffected(String resourceIdempotent) throws Exception {
		String idempotentElement = ResourceHarness.getTestResource(resourceIdempotent);
		return testUnaffected(resourceIdempotent, idempotentElement);
	}

	/**
	 * Asserts that the given elements in the resources directory are transformed as expected.
	 */
	public StepHarness testException(String resourceBefore, Consumer<AbstractThrowableAssert<?, ? extends Throwable>> exceptionAssertion) throws Exception {
		String before = ResourceHarness.getTestResource(resourceBefore);
		try {
			formatter.apply(before, new File(resourceBefore));
			Assert.fail();
		} catch (Throwable t) {
			AbstractThrowableAssert<?, ? extends Throwable> abstractAssert = Assertions.assertThat(t);
			exceptionAssertion.accept(abstractAssert);
		}
		return this;
	}

	@Override
	public void close() {
		if (formatter instanceof FormatterFunc.Closeable) {
			((FormatterFunc.Closeable) formatter).close();
		}
	}
}
