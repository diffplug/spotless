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

public class EndWithNewlineStepTest extends ResourceHarness {
	@Test
	public void trimTrailingNewlines() throws Exception {
		assertTask(extension -> extension.format("underTest", FormatExtension::endWithNewline), cases -> {
			cases.add("", "\n");
			cases.add("\n");
			cases.add("\n\n\n\n", "\n");
			cases.add("line", "line\n");
			cases.add("line\n");
			cases.add("line\nline\n\n\n\n", "line\nline\n");
		});
	}
}
