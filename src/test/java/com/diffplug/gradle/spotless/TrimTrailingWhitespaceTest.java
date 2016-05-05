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

public class TrimTrailingWhitespaceTest extends FormatExtensionTest {
	@Test
	public void trimTrailingWhitespace() throws Exception {
		trimTrailingWhitespaceTestCase("");
		trimTrailingWhitespaceTestCase("\n");
		trimTrailingWhitespaceTestCase("\n\n\n");
		trimTrailingWhitespaceTestCase("   preceding");

		trimTrailingWhitespaceTestCase("trailing  ", "trailing");
		trimTrailingWhitespaceTestCase("trailing  \n", "trailing\n");
		trimTrailingWhitespaceTestCase("trailing\t", "trailing");
		trimTrailingWhitespaceTestCase("trailing\t\n", "trailing\n");

		trimTrailingWhitespaceTestCase("\t  trailing  ", "\t  trailing");
		trimTrailingWhitespaceTestCase("\t  trailing  \n", "\t  trailing\n");
		trimTrailingWhitespaceTestCase("\t  trailing\t", "\t  trailing");
		trimTrailingWhitespaceTestCase("\t  trailing\t\n", "\t  trailing\n");

		trimTrailingWhitespaceTestCase("Line\nLine");
		trimTrailingWhitespaceTestCase("Line  \nLine", "Line\nLine");
		trimTrailingWhitespaceTestCase("Line\nLine  ", "Line\nLine");
		trimTrailingWhitespaceTestCase("Line  \nLine  ", "Line\nLine");
		trimTrailingWhitespaceTestCase("  Line  \nLine  ", "  Line\nLine");
		trimTrailingWhitespaceTestCase("  Line  \n  Line  ", "  Line\n  Line");

		trimTrailingWhitespaceTestCase("Line\r\nLine");
		trimTrailingWhitespaceTestCase("Line \r\nLine", "Line\r\nLine");
		trimTrailingWhitespaceTestCase("Line\r\nLine ", "Line\r\nLine");
		trimTrailingWhitespaceTestCase("Line \r\nLine ", "Line\r\nLine");
		trimTrailingWhitespaceTestCase(" Line \r\nLine ", " Line\r\nLine");
		trimTrailingWhitespaceTestCase(" Line \r\n Line ", " Line\r\n Line");
	}

	private void trimTrailingWhitespaceTestCase(String before) throws Exception {
		trimTrailingWhitespaceTestCase(before, before);
	}

	private void trimTrailingWhitespaceTestCase(String before, String after) throws Exception {
		super.assertTask(test -> {
			test.dontDoLineEndingNormalization();
			test.trimTrailingWhitespace();
		}, before, after);
	}
}
