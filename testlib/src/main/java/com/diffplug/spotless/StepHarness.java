/*
 * Copyright 2016-2024 DiffPlug
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
import java.nio.file.Paths;
import java.util.Arrays;

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;

/** An api for testing a {@code FormatterStep} that doesn't depend on the File path. DO NOT ADD FILE SUPPORT TO THIS, use {@link StepHarnessWithFile} if you need that. */
public class StepHarness extends StepHarnessBase {
	private StepHarness(Formatter formatter, RoundTrip roundTrip) {
		super(formatter, roundTrip);
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
				.rootDir(Paths.get(""))
				.exceptionPolicy(new FormatExceptionPolicyStrict())
				.build());
	}

	/** Creates a harness for testing a formatter whose steps don't depend on the file. */
	public static StepHarness forFormatter(Formatter formatter) {
		return new StepHarness(formatter, RoundTrip.ASSERT_EQUAL);
	}

	/** Asserts that the given element is transformed as expected, and that the result is idempotent. */
	public StepHarness test(String before, String after) {
		String actual = formatter().compute(LineEnding.toUnix(before), new File(""));
		assertEquals(after, actual, "Step application failed");
		return testUnaffected(after);
	}

	/** Asserts that the given element is idempotent w.r.t the step under test. */
	public StepHarness testUnaffected(String idempotentElement) {
		String actual = formatter().compute(LineEnding.toUnix(idempotentElement), new File(""));
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
		try {
			formatter().compute(LineEnding.toUnix(before), Formatter.NO_FILE_SENTINEL);
			throw new SecurityException("Expected exception");
		} catch (Throwable e) {
			if (e instanceof SecurityException) {
				throw new AssertionError(e.getMessage());
			} else {
				Throwable rootCause = e;
				while (rootCause.getCause() != null) {
					if (rootCause instanceof IllegalStateException) {
						break;
					}
					rootCause = rootCause.getCause();
				}
				return Assertions.assertThat(rootCause.getMessage());
			}
		}
	}
}
