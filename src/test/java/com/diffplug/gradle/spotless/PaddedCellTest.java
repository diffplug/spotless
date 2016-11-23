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

import static com.diffplug.gradle.spotless.PaddedCell.Type.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import com.diffplug.gradle.spotless.fi.SerializableThrowingFunction;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Throwing;

public class PaddedCellTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private void misbehaved(SerializableThrowingFunction<String, String> step, String input, PaddedCell.Type expectedOutputType, String steps, String canonical) throws IOException {
		testCase(step, input, expectedOutputType, steps, canonical, true);
	}

	private void wellBehaved(SerializableThrowingFunction<String, String> step, String input, PaddedCell.Type expectedOutputType, String canonical) throws IOException {
		testCase(step, input, expectedOutputType, canonical, canonical, false);
	}

	private void testCase(SerializableThrowingFunction<String, String> step, String input, PaddedCell.Type expectedOutputType, String expectedSteps, String canonical, boolean misbehaved) throws IOException {
		List<FormatterStep> formatterSteps = new ArrayList<>();
		formatterSteps.add(NonUpToDateCheckingTasks.create("step", step));
		Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(LineEnding.UNIX_POLICY)
				.encoding(StandardCharsets.UTF_8)
				.projectDirectory(folder.getRoot().toPath())
				.steps(formatterSteps).build();

		File file = folder.newFile();
		Files.write(file.toPath(), input.getBytes(StandardCharsets.UTF_8));

		PaddedCell result = PaddedCell.check(formatter, file);
		Assert.assertEquals(misbehaved, result.misbehaved());
		Assert.assertEquals(expectedOutputType, result.type());

		String actual = String.join(",", result.steps());
		Assert.assertEquals(expectedSteps, actual);

		if (canonical == null) {
			try {
				result.canonical();
				Assert.fail("Expected exception");
			} catch (IllegalArgumentException e) {}
		} else {
			Assert.assertEquals(canonical, result.canonical());
		}
	}

	@Test
	public void wellBehaved() throws IOException {
		wellBehaved(input -> input, "CCC", CONVERGE, "CCC");
		wellBehaved(input -> "A", "CCC", CONVERGE, "A");
	}

	@Test
	public void pingPong() throws IOException {
		misbehaved(input -> input.equals("A") ? "B" : "A", "CCC", CYCLE, "A,B", "A");
	}

	@Test
	public void fourState() throws IOException {
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
	public void converging() throws IOException {
		misbehaved(input -> {
			if (input.isEmpty()) {
				return input;
			} else {
				return input.substring(0, input.length() - 1);
			}
		}, "CCC", CONVERGE, "CC,C,", "");
	}

	@Test
	public void diverging() throws IOException {
		misbehaved(input -> input + " ", "", DIVERGE, " ,  ,   ,    ,     ,      ,       ,        ,         ,          ", null);
	}

	@Test
	public void cycleOrder() {
		BiConsumer<String, String> testCase = (unorderedStr, canonical) -> {
			List<String> unordered = Arrays.asList(unorderedStr.split(","));
			for (int i = 0; i < unordered.size(); ++i) {
				// try every rotation of the list
				Collections.rotate(unordered, 1);
				PaddedCell result = CYCLE.create(folder.getRoot(), unordered);
				// make sure the canonical result is always the appropriate one
				Assert.assertEquals(canonical, result.canonical());
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
