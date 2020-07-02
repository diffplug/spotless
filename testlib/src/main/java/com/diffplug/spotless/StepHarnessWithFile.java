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

/** An api for testing a `FormatterStep` that depends on the File path. */
public class StepHarnessWithFile implements AutoCloseable {
	private final FormatterFunc formatter;

	private StepHarnessWithFile(FormatterFunc formatter) {
		this.formatter = Objects.requireNonNull(formatter);
	}

	/** Creates a harness for testing steps which do depend on the file. */
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
					public String apply(String unix) throws Exception {
						return apply(unix, new File(""));
					}

					@Override
					public String apply(String unix, File file) throws Exception {
						return LineEnding.toUnix(step.format(unix, file));
					}
				}));
	}

	/** Creates a harness for testing a formatter whose steps do depend on the file. */
	public static StepHarnessWithFile forFormatter(Formatter formatter) {
		return new StepHarnessWithFile(FormatterFunc.Closeable.of(
				formatter::close,
				input -> formatter.compute(input, new File(""))));
	}

	/** Asserts that the given element is transformed as expected, and that the result is idempotent. */
	public StepHarnessWithFile test(File file, String before, String after) throws Exception {
		String actual = formatter.apply(before, file);
		Assert.assertEquals("Step application failed", after, actual);
		return testUnaffected(file, after);
	}

	/** Asserts that the given element is idempotent w.r.t the step under test. */
	public StepHarnessWithFile testUnaffected(File file, String idempotentElement) throws Exception {
		String actual = formatter.apply(idempotentElement, file);
		Assert.assertEquals("Step is not idempotent", idempotentElement, actual);
		return this;
	}

	/** Asserts that the given elements in  the resources directory are transformed as expected. */
	public StepHarnessWithFile testResource(File file, String resourceBefore, String resourceAfter) throws Exception {
		String before = ResourceHarness.getTestResource(resourceBefore);
		String after = ResourceHarness.getTestResource(resourceAfter);
		return test(file, before, after);
	}

	/** Asserts that the given elements in the resources directory are transformed as expected. */
	public StepHarnessWithFile testResourceUnaffected(File file, String resourceIdempotent) throws Exception {
		String idempotentElement = ResourceHarness.getTestResource(resourceIdempotent);
		return testUnaffected(file, idempotentElement);
	}

	/** Asserts that the given elements in the resources directory are transformed as expected. */
	public StepHarnessWithFile testException(File file, String resourceBefore, Consumer<AbstractThrowableAssert<?, ? extends Throwable>> exceptionAssertion) throws Exception {
		String before = ResourceHarness.getTestResource(resourceBefore);
		try {
			formatter.apply(before, file);
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
