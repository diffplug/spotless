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

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import com.diffplug.gradle.spotless.LineEndingStep.EOLNormalizer;

public class LineEndingStepTest extends ResourceTest {
	@Test
	@Ignore("This kind of test doesn't work until assertStep is refactored")
	public void normalizeToUnixFromFile() throws Throwable {
		LineEndingStep eol = new LineEndingStep(LineEnding.UNIX);
		assertStep(eol::format, "EOLWindows.test", "EOLUnix.test");
		assertStep(eol::format, "EOLMixed.test", "EOLUnix.test");
		assertStep(eol::format, "EOLUnix.test", "EOLUnix.test");
	}

	@Test
	@Ignore("This kind of test doesn't work until assertStep is refactored")
	public void normalizeToWindowsFromFile() throws Throwable {
		LineEndingStep eol = new LineEndingStep(LineEnding.WINDOWS);
		assertStep(eol::format, "EOLWindows.test", "EOLWindows.test");
		assertStep(eol::format, "EOLMixed.test", "EOLWindows.test");
		assertStep(eol::format, "EOLUnix.test", "EOLWindows.test");
	}

	@Test
	@Ignore("This kind of test doesn't work until assertStep is refactored")
	public void normalizeToDerivedEOLFromFile() throws Throwable {
		LineEndingStep eol = new LineEndingStep(LineEnding.DERIVED);
		assertStep(eol::format, "EOLWindows.test", "EOLWindows.test");
		assertStep(eol::format, "EOLMixed.test", "EOLWindows.test"); // first line contains '\r\n'
		assertStep(eol::format, "EOLUnix.test", "EOLUnix.test");
	}

	@Test
	@Ignore("This kind of test doesn't work until assertStep is refactored")
	public void normalizeToPlatformNativeFromFile_Windows() throws Throwable {
		LineEndingService lineEndingServiceMock = Mockito.mock(LineEndingService.class);
		Mockito.when(lineEndingServiceMock.getPlatformLineEnding()).thenReturn(LineEnding.WINDOWS);

		LineEndingStep eol = new LineEndingStep(LineEnding.PLATFORM_NATIVE, lineEndingServiceMock);
		assertStep(eol::format, "EOLWindows.test", "EOLWindows.test");
		assertStep(eol::format, "EOLMixed.test", "EOLWindows.test");
		assertStep(eol::format, "EOLUnix.test", "EOLWindows.test");
	}

	@Test
	@Ignore("This kind of test doesn't work until assertStep is refactored")
	public void normalizeToPlatformNativeFromFile_Unix() throws Throwable {
		LineEndingService lineEndingServiceMock = Mockito.mock(LineEndingService.class);
		Mockito.when(lineEndingServiceMock.getPlatformLineEnding()).thenReturn(LineEnding.UNIX);

		LineEndingStep eol = new LineEndingStep(LineEnding.PLATFORM_NATIVE, lineEndingServiceMock);
		assertStep(eol::format, "EOLWindows.test", "EOLUnix.test");
		assertStep(eol::format, "EOLMixed.test", "EOLUnix.test");
		assertStep(eol::format, "EOLUnix.test", "EOLUnix.test");
	}

	@Test(expected = IllegalStateException.class)
	@Ignore("This kind of test doesn't work until assertStep is refactored")
	public void normalizeToPlatformNativeFromFile_UnexpectedServiceResult() throws Throwable {
		LineEndingService lineEndingServiceMock = Mockito.mock(LineEndingService.class);
		Mockito.when(lineEndingServiceMock.getPlatformLineEnding()).thenReturn(LineEnding.UNCERTAIN);

		new LineEndingStep(LineEnding.PLATFORM_NATIVE, lineEndingServiceMock);
	}

	@Test
	public void normalizeToUnix() {
		EOLNormalizer normalizer = new LineEndingStep.UnixEOLNormalizer();
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \r\nline 3", "line 1\nline 2 \nline 3");
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \nline 3", "line 1\nline 2 \nline 3");
		assertEOLnormalization(normalizer, "line 1\nline 2 \nline 3", "line 1\nline 2 \nline 3");
	}

	@Test
	public void normalizeToWindows() {
		EOLNormalizer normalizer = new LineEndingStep.WindowsEOLNormalizer();
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \r\nline 3", "line 1\r\nline 2 \r\nline 3");
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \nline 3", "line 1\r\nline 2 \r\nline 3");
		assertEOLnormalization(normalizer, "line 1\nline 2 \nline 3", "line 1\r\nline 2 \r\nline 3");
	}

	@Test
	public void normalizeToDerivedEOL() {
		EOLNormalizer normalizer = new LineEndingStep.DerivedEOLNormalizer(new LineEndingService());
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \r\nline 3", "line 1\r\nline 2 \r\nline 3");
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \nline 3", "line 1\r\nline 2 \r\nline 3");
		assertEOLnormalization(normalizer, "line 1\nline 2 \nline 3", "line 1\nline 2 \nline 3");
	}

	@Test
	public void normalizeToPlatformNative() {
		LineEndingService lineEndingServiceMock = Mockito.mock(LineEndingService.class);
		Mockito.when(lineEndingServiceMock.getPlatformLineEnding()).thenReturn(LineEnding.UNIX);
		EOLNormalizer normalizer = new LineEndingStep.PlatformNativeEOLNormalizer(lineEndingServiceMock);
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \r\nline 3", "line 1\nline 2 \nline 3");
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \nline 3", "line 1\nline 2 \nline 3");
		assertEOLnormalization(normalizer, "line 1\nline 2 \nline 3", "line 1\nline 2 \nline 3");

		lineEndingServiceMock = Mockito.mock(LineEndingService.class);
		Mockito.when(lineEndingServiceMock.getPlatformLineEnding()).thenReturn(LineEnding.WINDOWS);
		normalizer = new LineEndingStep.PlatformNativeEOLNormalizer(lineEndingServiceMock);
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \r\nline 3", "line 1\r\nline 2 \r\nline 3");
		assertEOLnormalization(normalizer, "line 1\r\nline 2 \nline 3", "line 1\r\nline 2 \r\nline 3");
		assertEOLnormalization(normalizer, "line 1\nline 2 \nline 3", "line 1\r\nline 2 \r\nline 3");
	}

	@Test(expected = IllegalStateException.class)
	public void normalizeToPlatformNative_UnexpectedServiceResult() {
		LineEndingService lineEndingServiceMock = Mockito.mock(LineEndingService.class);
		Mockito.when(lineEndingServiceMock.getPlatformLineEnding()).thenReturn(LineEnding.UNCERTAIN);
		new LineEndingStep.PlatformNativeEOLNormalizer(lineEndingServiceMock);
	}

	private void assertEOLnormalization(LineEndingStep.EOLNormalizer normalizer, String before, String expectedAfter) {
		String after = normalizer.format(before);
		assertThat(after, is(expectedAfter));
	}
}
