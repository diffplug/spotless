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

import org.junit.Test;

public class LineEndingTest extends FormatExtensionTest {
	@Test
	public void lineEndingNormalizationIsActiveByDefault() throws Exception {
		testNormalization(LineEnding.UNIX, "line\r\n", "line\n");
		testNormalization(LineEnding.UNIX, "line\n", "line\n");
		testNormalization(LineEnding.WINDOWS, "line\n", "line\r\n");
		testNormalization(LineEnding.WINDOWS, "line\r\n", "line\r\n");
		testNormalization(LineEnding.DERIVED, "line\r\nfoobar\n", "line\r\nfoobar\r\n");
	}

	private void testNormalization(LineEnding lineEnding, String before, String after) throws Exception {
		super.assertTask(test -> {
			test.root.setLineEndings(lineEnding);
		}, before, after);
	}
}
