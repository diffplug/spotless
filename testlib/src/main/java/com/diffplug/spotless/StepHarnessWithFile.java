/*
 * Copyright 2016-2025 DiffPlug
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;

import com.diffplug.selfie.StringSelfie;

/** An api for testing a {@code FormatterStep} that depends on the File path. */
public final class StepHarnessWithFile extends StepHarnessBase {
	private final ResourceHarness harness;

	private StepHarnessWithFile(ResourceHarness harness, Formatter formatter, RoundTrip roundTrip) {
		super(formatter, roundTrip);
		this.harness = Objects.requireNonNull(harness);
	}

	/** Creates a harness for testing steps which do depend on the file. */
	public static StepHarnessWithFile forStep(ResourceHarness harness, FormatterStep step) {
		return forFormatter(harness, Formatter.builder()
				.encoding(StandardCharsets.UTF_8)
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.steps(Collections.singletonList(step))
				.build());
	}

	/** Creates a harness for testing a formatter whose steps do depend on the file. */
	public static StepHarnessWithFile forFormatter(ResourceHarness harness, Formatter formatter) {
		return new StepHarnessWithFile(harness, formatter, RoundTrip.ASSERT_EQUAL);
	}

	/** Asserts that the given element is transformed as expected, and that the result is idempotent. */
	public StepHarnessWithFile test(String filename, String before, String after) {
		File file = harness.setFile(filename).toContent(before);
		String actual = formatter().compute(LineEnding.toUnix(before), file);
		assertEquals(after, actual, "Step application failed");
		return testUnaffected(file, after);
	}

	/** Asserts that the given element is idempotent w.r.t the step under test. */
	public StepHarnessWithFile testUnaffected(File file, String idempotentElement) {
		String actual = formatter().compute(LineEnding.toUnix(idempotentElement), file);
		assertEquals(idempotentElement, actual, "Step is not idempotent");
		return this;
	}

	/** Asserts that the given element is idempotent w.r.t the step under test. */
	public StepHarnessWithFile testUnaffected(String file, String idempotentElement) {
		return testUnaffected(harness.setFile(file).toContent(idempotentElement), idempotentElement);
	}

	/** Asserts that the given elements in  the resources directory are transformed as expected. */
	public StepHarnessWithFile testResource(String resourceBefore, String resourceAfter) {
		return testResource(resourceBefore, resourceBefore, resourceAfter);
	}

	public StepHarnessWithFile testResource(String filename, String resourceBefore, String resourceAfter) {
		String contentBefore = ResourceHarness.getTestResource(resourceBefore);
		return test(filename, contentBefore, ResourceHarness.getTestResource(resourceAfter));
	}

	/** Asserts that the given elements in the resources directory are transformed as expected. */
	public StepHarnessWithFile testResourceUnaffected(String resourceIdempotent) {
		String contentBefore = ResourceHarness.getTestResource(resourceIdempotent);
		File file = harness.setFile(resourceIdempotent).toContent(contentBefore);
		return testUnaffected(file, contentBefore);
	}

	public StringSelfie expectLintsOfResource(String resource) {
		return expectLintsOfResource(resource, resource);
	}

	public StringSelfie expectLintsOfResource(String filename, String resource) {
		try {
			File file = harness.setFile(filename).toResource(resource);
			LintState state = LintState.of(formatter(), file);
			return StepHarness.expectLintsOf(state, formatter());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}

	public StringSelfie expectLintsOfFileAndContent(String filename, String content) {
		try {
			File file = harness.setFile(filename).toContent(content);
			LintState state = LintState.of(formatter(), file);
			return StepHarness.expectLintsOf(state, formatter());
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
}
