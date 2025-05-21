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
package com.diffplug.spotless.extra.middleware;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.Lint;

public class ReviewDogGeneratorTest {

	@Test
	public void diffSingleLine() {
		String result = ReviewDogGenerator.rdjsonlDiff("test.txt", "dirty", "clean");
		assertNotNull(result);
		assertTrue(result.contains("\"path\":\"test.txt\""));
		assertTrue(result.contains("\"diff\":"));
		assertTrue(result.contains("-dirty"));
		assertTrue(result.contains("+clean"));
	}

	@Test
	public void diffNoChange() {
		String result = ReviewDogGenerator.rdjsonlDiff("test.txt", "same", "same");
		assertEquals("", result);
	}

	@Test
	public void diffMultipleLines() {
		String actual = "Line 1\nLine 2\nDirty line\nLine 4";
		String formatted = "Line 1\nLine 2\nClean line\nLine 4";

		String result = ReviewDogGenerator.rdjsonlDiff("src/main.java", actual, formatted);

		assertNotNull(result);
		assertTrue(result.contains("\"path\":\"src/main.java\""));
		assertTrue(result.contains("-Dirty line"));
		assertTrue(result.contains("+Clean line"));
	}

	@Test
	public void lintsEmpty() {
		List<FormatterStep> steps = new ArrayList<>();
		List<List<Lint>> lintsPerStep = new ArrayList<>();

		String result = ReviewDogGenerator.rdjsonlLints("test.txt", "content", steps, lintsPerStep);
		assertEquals("", result);
	}

	@Test
	public void lintsSingleIssue() {
		FormatterStep step = FormatterStep.create(
				"testStep",
				"formatter-state",
				state -> rawUnix -> rawUnix);
		List<FormatterStep> steps = Collections.singletonList(step);

		Lint lint = Lint.atLine(1, "TEST001", "Test lint message");
		List<List<Lint>> lintsPerStep = Collections.singletonList(Collections.singletonList(lint));

		String result = ReviewDogGenerator.rdjsonlLints("src/main.java", "content", steps, lintsPerStep);

		assertNotNull(result);
		assertTrue(result.contains("\"path\":\"src/main.java\""));
		assertTrue(result.contains("\"line\":1"));
		assertTrue(result.contains("TEST001: Test lint message"));
	}

	@Test
	public void lintsMultipleIssues() {
		FormatterStep step1 = new FormatterStep() {
			@Override
			public String getName() {
				return "step1";
			}

			@Override
			public String format(String rawUnix, File file) {
				return rawUnix;
			}

			@Override
			public void close() {}
		};

		FormatterStep step2 = FormatterStep.create(
				"step2",
				"formatter-state",
				state -> rawUnix -> rawUnix);

		List<FormatterStep> steps = Arrays.asList(step1, step2);

		Lint lint1 = Lint.atLine(1, "RULE1", "First issue");
		Lint lint2 = Lint.atLine(5, "RULE2", "Second issue");

		List<List<Lint>> lintsPerStep = Arrays.asList(
				Collections.singletonList(lint1),
				Collections.singletonList(lint2));

		String result = ReviewDogGenerator.rdjsonlLints("src/main.java", "content", steps, lintsPerStep);

		assertNotNull(result);
		assertTrue(result.contains("RULE1"));
		assertTrue(result.contains("RULE2"));
		assertTrue(result.contains("First issue"));
		assertTrue(result.contains("Second issue"));

		String[] lines = result.split("\n");
		assertEquals(2, lines.length);
	}
}
