/*
 * Copyright 2016 DiffPlug
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

import org.junit.Assert;

/** An api for adding test cases. */
public class StepHarness {
	private final FormatterFunc formatter;

	/** Creates a StepHarness around the given FormatterFunc. */
	public StepHarness(FormatterFunc formatter) {
		this.formatter = Objects.requireNonNull(formatter);
	}

	/** Creates a harness for testing steps which don't depend on the file. */
	public static StepHarness forStep(FormatterStep step) {
		return new StepHarness(input -> step.format(input, new File("")));
	}

	/** Creates a harness for testing a formatter whose steps don't depend on the file. */
	public static StepHarness forFormatter(Formatter formatter) {
		return new StepHarness(input -> formatter.compute(input, new File("")));
	}

	/** Asserts that the given element is transformed as expected, and that the result is idempotent. */
	public StepHarness test(String before, String after) throws Exception {
		String actual = formatter.apply(before);
		Assert.assertEquals("Step application failed", after, actual);
		return testUnaffected(after);
	}

	/** Asserts that the given element is idempotent w.r.t the step under test. */
	public StepHarness testUnaffected(String idempotentElement) throws Exception {
		String actual = formatter.apply(idempotentElement);
		Assert.assertEquals("Step is not idempotent", idempotentElement, actual);
		return this;
	}

	/** Asserts that the given elements in  the resources directory are transformed as expected. */
	public StepHarness testResource(String resourceBefore, String resourceAfter) throws Exception {
		String before = ResourceHarness.getTestResource(resourceBefore);
		String after = ResourceHarness.getTestResource(resourceAfter);
		return test(before, after);
	}

	/** Asserts that the given elements in the resources directory are transformed as expected. */
	public StepHarness testResourceUnaffected(String resourceIdempotent) throws Exception {
		String idempotentElement = ResourceHarness.getTestResource(resourceIdempotent);
		return testUnaffected(idempotentElement);
	}
}
