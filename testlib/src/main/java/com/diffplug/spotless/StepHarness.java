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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Arrays;

import com.diffplug.selfie.Selfie;
import com.diffplug.selfie.StringSelfie;

/** An api for testing a {@code FormatterStep} that doesn't depend on the File path. DO NOT ADD FILE SUPPORT TO THIS, use {@link StepHarnessWithFile} if you need that. */
public final class StepHarness extends StepHarnessBase {
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
				.encoding(UTF_8)
				.build());
	}

	/** Creates a harness for testing a formatter whose steps don't depend on the file. */
	public static StepHarness forFormatter(Formatter formatter) {
		return new StepHarness(formatter, RoundTrip.ASSERT_EQUAL);
	}

	public static StepHarness forStepNoRoundtrip(FormatterStep step) {
		return new StepHarness(Formatter.builder()
				.steps(Arrays.asList(step))
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(UTF_8)
				.build(), RoundTrip.DONT_ROUNDTRIP);
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
		String actual = formatter().compute(LineEnding.toUnix(before), new File(resourceBefore));
		assertEquals(after, actual, "Step application failed");
		actual = formatter().compute(LineEnding.toUnix(after), new File(resourceAfter));
		assertEquals(after, actual, "Step is not idempotent");
		return this;
	}

	/** Asserts that the given elements in the resources directory are transformed as expected. */
	public StepHarness testResourceUnaffected(String resourceIdempotent) {
		String idempotentElement = ResourceHarness.getTestResource(resourceIdempotent);
		return testUnaffected(idempotentElement);
	}

	public StringSelfie expectLintsOfResource(String before) {
		return expectLintsOf(ResourceHarness.getTestResource(before));
	}

	public StringSelfie expectLintsOf(String before) {
		LintState state = LintState.of(formatter(), Formatter.NO_FILE_SENTINEL, before.getBytes(formatter().getEncoding()));
		return expectLintsOf(state, formatter());
	}

	static StringSelfie expectLintsOf(LintState state, Formatter formatter) {
		String assertAgainst = state.asStringOneLine(Formatter.NO_FILE_SENTINEL, formatter);
		String cleaned = assertAgainst.replace("NO_FILE_SENTINEL:", "");

		int numLines = 1;
		int lineEnding = cleaned.indexOf('\n');
		while (lineEnding != -1 && numLines < 10) {
			++numLines;
			lineEnding = cleaned.indexOf('\n', lineEnding + 1);
		}
		String toSnapshot = lineEnding == -1 ? cleaned : (cleaned.substring(0, lineEnding) + "\n(... and more)");
		return Selfie.expectSelfie(toSnapshot);
	}
}
