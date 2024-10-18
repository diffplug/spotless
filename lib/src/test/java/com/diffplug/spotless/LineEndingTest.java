/*
 * Copyright 2024 DiffPlug
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LineEndingTest {

	@Test
	void testGetEndingFor() throws IOException {
		assertLineEnding("\r", "\r");
		assertLineEnding("\r", "Test\r");
		assertLineEnding("\r", "Test\rTest2\n");

		assertLineEnding("\n", "Test");

		assertLineEnding("\r\n", "\r\n");
		assertLineEnding("\r\n", "Test\r\n");
		assertLineEnding("\r\n", "Test\r\nTest2\n");

		assertLineEnding("\n", "\n");
		assertLineEnding("\n", "Test\n");
		assertLineEnding("\n", "Test\nTest2\r");
		assertLineEnding("\n", "\n\t");
	}

	static void assertLineEnding(String ending, String input) throws IOException {
		try (Reader reader = new StringReader(input)) {
			Assertions.assertEquals(ending, LineEnding.PreserveLineEndingPolicy.getEndingFor(reader));
		}
	}
}
