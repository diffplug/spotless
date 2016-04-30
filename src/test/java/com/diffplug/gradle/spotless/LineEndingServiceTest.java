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

public class LineEndingServiceTest {

	private LineEndingService classUnderTest = new LineEndingService();

	@Test
	public void testDetermineLineEnding() {
		assertLineEndingDetermination("foobar\r\nhello world\r\n", LineEnding.WINDOWS);
		assertLineEndingDetermination("foobar\r\nhello world\n", LineEnding.WINDOWS);
		assertLineEndingDetermination("foobar\r\n", LineEnding.WINDOWS);

		assertLineEndingDetermination("foobar\nhello world\n", LineEnding.UNIX);
		assertLineEndingDetermination("foobar\nhello world\r\n", LineEnding.UNIX);
		assertLineEndingDetermination("foobar\n", LineEnding.UNIX);

		assertLineEndingDetermination("foobar", LineEnding.UNCERTAIN);

		// Don't care about legacy Mac EOL
		assertLineEndingDetermination("foobar\r", LineEnding.UNCERTAIN);
	}

	private void assertLineEndingDetermination(String raw, LineEnding expectedLineEnding) {
		LineEnding lineEnding = classUnderTest.determineLineEnding(raw);

		assertThat(lineEnding, is(expectedLineEnding));
	}

	@Test
	public void testGetPlatformLineSeparator_Unix() {
		classUnderTest.setLineSeparator(() -> "\n");

		String separator = classUnderTest.getPlatformLineSeparator();

		assertThat(separator, is("\n"));
	}

	@Test
	public void testGetPlatformLineSeparator_Windows() {
		classUnderTest.setLineSeparator(() -> "\r\n");

		String separator = classUnderTest.getPlatformLineSeparator();

		assertThat(separator, is("\r\n"));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetPlatformLineSeparator_dubious() {
		classUnderTest.setLineSeparator(() -> "foo");

		classUnderTest.getPlatformLineSeparator();
	}
}
