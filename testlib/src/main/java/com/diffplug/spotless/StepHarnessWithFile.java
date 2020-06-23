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
 * An api for adding test cases making use of the file currently formatted.
 */
public class StepHarnessWithFile implements AutoCloseable {
	private final FormatterFunc formatter;

	/**
	 * Creates a StepHarness around the given FormatterFunc.
	 */
	private StepHarnessWithFile(FormatterFunc formatter) {
		this.formatter = Objects.requireNonNull(formatter);
	}

	/**
	 * Creates a harness for testing steps which do depend on the file.
	 */
	public static StepHarnessWithFile forStep(FormatterStep step) {
		// We don't care if an individual FormatterStep is misbehaving on line-endings, because
		// Formatter fixes that.  No reason to care in tests either.  It's likely to pop up when
		// running tests on Windows from time-to-time
		return new StepHarnessWithFile(FormatterFunc.Closeable.of(
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
	 * Asserts that the given resource containing element is transformed as expected, and that the result is idempotent.
	 */
	public StepHarnessWithFile test(String resourceBefore, String before, String resourceAfter, String after) throws Exception {
		String actual = formatter.apply(before, new File(resourceBefore));
		Assert.assertEquals("Step application failed", after, actual);
		return testUnaffected(resourceAfter, after);

	}

	/**
	 * Asserts that the given resource containing element is idempotent w.r.t the step under test.
	 */
	public StepHarnessWithFile testUnaffected(String resourceIdempotent, String idempotentElement) throws Exception {
		String actual = formatter.apply(idempotentElement, new File(resourceIdempotent));
		Assert.assertEquals("Step is not idempotent", idempotentElement, actual);
		return this;
	}

	/**
	 * Asserts that the given elements in  the resources directory are transformed as expected.
	 */
	public StepHarnessWithFile testResource(String resourceBefore, String resourceAfter) throws Exception {
		String before = ResourceHarness.getTestResource(resourceBefore);
		String after = ResourceHarness.getTestResource(resourceAfter);
		return test(resourceBefore, before, resourceAfter, after);
	}

	/**
	 * Asserts that the given elements in the resources directory are transformed as expected.
	 */
	public StepHarnessWithFile testResourceUnaffected(String resourceIdempotent) throws Exception {
		String idempotentElement = ResourceHarness.getTestResource(resourceIdempotent);
		return testUnaffected(resourceIdempotent, idempotentElement);
	}

	/**
	 * Asserts that the given elements in the resources directory are transformed as expected.
	 */
	public StepHarnessWithFile testException(String resourceBefore, Consumer<AbstractThrowableAssert<?, ? extends Throwable>> exceptionAssertion) throws Exception {
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
