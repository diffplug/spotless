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
package com.diffplug.gradle.spotless;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class FileEndingStepTest {
	@Test
	public void testFormat_Windows() {
		FileEndingStep classUnderTest = new FileEndingStep(LineEnding.WINDOWS);
		endWithNewlineTest(classUnderTest, "", "\r\n");
		endWithNewlineTest(classUnderTest, "\n", "\r\n");
		endWithNewlineTest(classUnderTest, "\n\n\t\r\n\n", "\r\n");
		endWithNewlineTest(classUnderTest, "line", "line\r\n");
		endWithNewlineTest(classUnderTest, "line\n", "line\r\n");
		endWithNewlineTest(classUnderTest, "line\nline\n\n\n\n", "line\nline\r\n");
		endWithNewlineTest(classUnderTest, "line\nline\r\n\n\t\n\n", "line\nline\r\n");
	}

	@Test
	public void testFormat_Unix() {
		FileEndingStep classUnderTest = new FileEndingStep(LineEnding.UNIX);
		endWithNewlineTest(classUnderTest, "", "\n");
		endWithNewlineTest(classUnderTest, "\n", "\n");
		endWithNewlineTest(classUnderTest, "\n\n\t\r\n\n", "\n");
		endWithNewlineTest(classUnderTest, "line", "line\n");
		endWithNewlineTest(classUnderTest, "line\n", "line\n");
		endWithNewlineTest(classUnderTest, "line\nline\n\n\n\n", "line\nline\n");
		endWithNewlineTest(classUnderTest, "line\nline\r\n\n\t\n\n", "line\nline\n");
	}

	@Test
	public void testFormat_Derived() {
		LineEndingService lineEndingService = new LineEndingService();
		lineEndingService.setLineSeparator(() -> "\r\n"); // platform native separator for the fallback

		FileEndingStep classUnderTest = new FileEndingStep(LineEnding.DERIVED, lineEndingService);
		endWithNewlineTest(classUnderTest, "", "\r\n"); // Fallback is here in use
		endWithNewlineTest(classUnderTest, "\n", "\r\n");
		endWithNewlineTest(classUnderTest, "\n\n\t\r\n\n", "\n");
		endWithNewlineTest(classUnderTest, "line", "line\r\n"); // and here
		endWithNewlineTest(classUnderTest, "line\n", "line\n");
		endWithNewlineTest(classUnderTest, "line\nline\n\n\n\n", "line\nline\n");
		endWithNewlineTest(classUnderTest, "line\nline\r\n\n\t\n\n", "line\nline\n");
	}

	@Test
	public void testFormat_ClobberDisabled() {
		FileEndingStep classUnderTest = new FileEndingStep(LineEnding.UNIX);
		classUnderTest.disableClobber();

		endWithNewlineTest(classUnderTest, "", "\n");
		endWithNewlineTest(classUnderTest, "\n", "\n");
		endWithNewlineTest(classUnderTest, "\n\n\t\r\n\n", "\n\n\t\r\n\n");
		endWithNewlineTest(classUnderTest, "line", "line\n");
		endWithNewlineTest(classUnderTest, "line\n", "line\n");
		endWithNewlineTest(classUnderTest, "line\nline\n\n\n\n", "line\nline\n\n\n\n");
		endWithNewlineTest(classUnderTest, "line\nline\r\n\n\t\n\n", "line\nline\r\n\n\t\n\n");

		classUnderTest = new FileEndingStep(LineEnding.WINDOWS);
		classUnderTest.disableClobber();

		endWithNewlineTest(classUnderTest, "line\nline\n\n\n\n", "line\nline\n\n\n\n\r\n");
	}

	private void endWithNewlineTest(FileEndingStep step, String before, String expectedAfter) {
		String after = step.format(before);
		assertThat(after, is(expectedAfter));
	}
}
