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

import static com.diffplug.spotless.PaddedCell.Type.CONVERGE;
import static com.diffplug.spotless.PaddedCell.Type.CYCLE;
import static com.diffplug.spotless.PaddedCell.Type.DIVERGE;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PaddedCellTest {
	@TempDir
	File rootFolder;

	private void misbehaved(SerializedFunction<String, String> step, String input, PaddedCell.Type expectedOutputType, String steps, String canonical) throws IOException {
		testCase(step, input, expectedOutputType, steps, canonical, true);
	}

	private void wellBehaved(SerializedFunction<String, String> step, String input, PaddedCell.Type expectedOutputType, String canonical) throws IOException {
		testCase(step, input, expectedOutputType, canonical, canonical, false);
	}

	private void testCase(SerializedFunction<String, String> step, String input, PaddedCell.Type expectedOutputType, String expectedSteps, String canonical, boolean misbehaved) throws IOException {
		List<FormatterStep> formatterSteps = new ArrayList<>();
		formatterSteps.add(NeverUpToDateStep.create("step", step));
		try (Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(LineEnding.UNIX.createPolicy())
				.encoding(StandardCharsets.UTF_8)
				.steps(formatterSteps).build()) {

			File file = new File(rootFolder, "input");
			Files.write(file.toPath(), input.getBytes(StandardCharsets.UTF_8));

			PaddedCell result = PaddedCell.check(formatter, file);
			Assertions.assertEquals(misbehaved, result.misbehaved());
			Assertions.assertEquals(expectedOutputType, result.type());

			String actual = String.join(",", result.steps());
			Assertions.assertEquals(expectedSteps, actual);

			if (canonical == null) {
				Assertions.assertThrows(IllegalArgumentException.class, result::canonical);
			} else {
				Assertions.assertEquals(canonical, result.canonical());
			}
		}
	}

	@Test
	void wellBehaved() throws IOException {
		wellBehaved(input -> input, "CCC", CONVERGE, "CCC");
		wellBehaved(input -> "A", "CCC", CONVERGE, "A");
	}

	@Test
	void pingPong() throws IOException {
		misbehaved(input -> input.equals("A") ? "B" : "A", "CCC", CYCLE, "A,B", "A");
	}

	@Test
	void fourState() throws IOException {
		misbehaved(input -> {
			// @formatter:off
			switch (input) {
			case "A": return "B";
			case "B": return "C";
			case "C": return "D";
			default:  return "A";
			}
			// @formatter:on
		}, "CCC", CYCLE, "A,B,C,D", "A");
	}

	@Test
	void converging() throws IOException {
		misbehaved(input -> {
			if (input.isEmpty()) {
				return input;
			} else {
				return input.substring(0, input.length() - 1);
			}
		}, "CCC", CONVERGE, "CC,C,", "");
	}

	@Test
	void diverging() throws IOException {
		misbehaved(input -> input + " ", "", DIVERGE, " ,  ,   ,    ,     ,      ,       ,        ,         ,          ", null);
	}

	@Test
	void cycleOrder() {
		BiConsumer<String, String> testCase = (unorderedStr, canonical) -> {
			List<String> unordered = Arrays.asList(unorderedStr.split(","));
			for (int i = 0; i < unordered.size(); i++) {
				// try every rotation of the list
				Collections.rotate(unordered, 1);
				PaddedCell result = CYCLE.create(rootFolder, unordered);
				// make sure the canonical result is always the appropriate one
				Assertions.assertEquals(canonical, result.canonical());
			}
		};
		// alphabetic
		testCase.accept("a,b,c", "a");
		// length
		testCase.accept("a,aa,aaa", "a");
		// length > alphabetic
		testCase.accept("b,aa,aaa", "b");
	}
}
