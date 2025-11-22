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

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.generic.EndWithNewlineStep;

class FormatterTest {
	@Test
	void toUnix() {
		assertEquals("1\n2\n3", LineEnding.toUnix("1\n2\n3"));
		assertEquals("1\n2\n3", LineEnding.toUnix("1\r2\r3"));
		assertEquals("1\n2\n3", LineEnding.toUnix("1\r\n2\r\n3"));
	}

	// Formatter normally needs to be closed, but no resources will be leaked in this special case
	@Test
	void equality() {
		new SerializableEqualityTester() {
			private LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX.createPolicy();
			private Charset encoding = UTF_8;
			private List<FormatterStep> steps = new ArrayList<>();

			@Override
			protected void setupTest(API api) throws Exception {
				api.areDifferentThan();

				lineEndingsPolicy = LineEnding.WINDOWS.createPolicy();
				api.areDifferentThan();

				encoding = UTF_16;
				api.areDifferentThan();

				steps.add(EndWithNewlineStep.create());
				api.areDifferentThan();
			}

			@Override
			protected Formatter create() {
				return Formatter.builder()
						.lineEndingsPolicy(lineEndingsPolicy)
						.encoding(encoding)
						.steps(steps)
						.build();
			}
		}.testEquals();
	}
}
