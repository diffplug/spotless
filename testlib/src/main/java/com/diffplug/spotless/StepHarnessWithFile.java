/*
 * Copyright 2016-2023 DiffPlug
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
import java.util.Collections;
import java.util.Objects;

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;

/** An api for testing a {@code FormatterStep} that depends on the File path. */
public class StepHarnessWithFile extends StepHarnessBase<StepHarnessWithFile> {
	private final ResourceHarness harness;

	private StepHarnessWithFile(ResourceHarness harness, Formatter formatter) {
		super(formatter);
		this.harness = Objects.requireNonNull(harness);
	}

	/** Creates a harness for testing steps which do depend on the file. */
	public static StepHarnessWithFile forStep(ResourceHarness harness, FormatterStep step) {
		return new StepHarnessWithFile(harness, Formatter.builder()
				.name(step.getName())
				.encoding(StandardCharsets.UTF_8)
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.steps(Collections.singletonList(step))
				.rootDir(harness.rootFolder().toPath())
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.build());
	}

	/** Creates a harness for testing a formatter whose steps do depend on the file. */
	public static StepHarnessWithFile forFormatter(ResourceHarness harness, Formatter formatter) {
		return new StepHarnessWithFile(harness, formatter);
	}

	/** Asserts that the given element is transformed as expected, and that the result is idempotent. */
	public StepHarnessWithFile test(File file, String before, String after) {
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

	/** Asserts that the given elements in  the resources directory are transformed as expected. */
	public StepHarnessWithFile testResource(String resourceBefore, String resourceAfter) {
		return testResource(resourceBefore, resourceBefore, resourceAfter);
	}

	public StepHarnessWithFile testResource(String filename, String resourceBefore, String resourceAfter) {
		String contentBefore = ResourceHarness.getTestResource(resourceBefore);
		File file = harness.setFile(filename).toContent(contentBefore);
		return test(file, contentBefore, ResourceHarness.getTestResource(resourceAfter));
	}

	/** Asserts that the given elements in the resources directory are transformed as expected. */
	public StepHarnessWithFile testResourceUnaffected(String resourceIdempotent) {
		String contentBefore = ResourceHarness.getTestResource(resourceIdempotent);
		File file = harness.setFile(resourceIdempotent).toContent(contentBefore);
		return testUnaffected(file, contentBefore);
	}

	public AbstractStringAssert<?> testResourceExceptionMsg(String resourceBefore) {
		return testResourceExceptionMsg(resourceBefore, resourceBefore);
	}

	public AbstractStringAssert<?> testResourceExceptionMsg(String filename, String resourceBefore) {
		String contentBefore = ResourceHarness.getTestResource(resourceBefore);
		File file = harness.setFile(filename).toContent(contentBefore);
		return testExceptionMsg(file, contentBefore);
	}

	public AbstractStringAssert<?> testExceptionMsg(File file, String before) {
		try {
			formatter().compute(LineEnding.toUnix(before), file);
			throw new SecurityException("Expected exception");
		} catch (Throwable e) {
			if (e instanceof SecurityException) {
				throw new AssertionError(e.getMessage());
			} else {
				Throwable rootCause = e;
				while (rootCause.getCause() != null) {
					rootCause = rootCause.getCause();
				}
				return Assertions.assertThat(rootCause.getMessage());
			}
		}
	}
}
