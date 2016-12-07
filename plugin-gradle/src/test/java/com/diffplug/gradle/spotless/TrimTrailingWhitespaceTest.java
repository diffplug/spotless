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

public class TrimTrailingWhitespaceTest extends GradleResourceHarness {
	@Test
	public void trimTrailingWhitespace() throws Exception {
		assertTask(extension -> extension.format("underTest", FormatExtension::trimTrailingWhitespace), cases -> {
			cases.add("");
			cases.add("\n");
			cases.add("\n\n\n");
			cases.add("   preceding");

			cases.add("trailing  ", "trailing");
			cases.add("trailing  \n", "trailing\n");
			cases.add("trailing\t", "trailing");
			cases.add("trailing\t\n", "trailing\n");

			cases.add("\t  trailing  ", "\t  trailing");
			cases.add("\t  trailing  \n", "\t  trailing\n");
			cases.add("\t  trailing\t", "\t  trailing");
			cases.add("\t  trailing\t\n", "\t  trailing\n");

			cases.add("Line\nLine");
			cases.add("Line  \nLine", "Line\nLine");
			cases.add("Line\nLine  ", "Line\nLine");
			cases.add("Line  \nLine  ", "Line\nLine");
			cases.add("  Line  \nLine  ", "  Line\nLine");
			cases.add("  Line  \n  Line  ", "  Line\n  Line");
		});
	}
}
