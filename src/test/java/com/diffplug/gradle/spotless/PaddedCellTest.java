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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Throwing;
import com.diffplug.gradle.spotless.PaddedCell.Converge;
import com.diffplug.gradle.spotless.PaddedCell.Cycle;
import com.diffplug.gradle.spotless.PaddedCell.Diverge;
import com.diffplug.gradle.spotless.PaddedCell.Result;

public class PaddedCellTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private <T> T testCase(Throwing.Function<String, String> step, String input, Class<T> expectedOutputType) throws IOException {
		List<FormatterStep> steps = new ArrayList<>();
		steps.add(FormatterStep.create("step", step));
		Formatter formatter = new Formatter(LineEnding.UNIX_POLICY, folder.getRoot().toPath(), steps);

		File file = folder.newFile();
		Files.write(file.toPath(), input.getBytes(StandardCharsets.UTF_8));

		PaddedCell padded = new PaddedCell(formatter, file);
		PaddedCell.Result result = padded.cycle();
		return result.as(expectedOutputType);
	}

	@Test
	public void wellBehaved() throws IOException {
		Converge alreadyClean = testCase(input -> input, "CCC", Converge.class);
		Assert.assertEquals("CCC", alreadyClean.convergesTo());
		Assert.assertEquals(0, alreadyClean.afterIterations());

		Converge oneIteration = testCase(input -> "A", "CCC", Converge.class);
		Assert.assertEquals("A", oneIteration.convergesTo());
		Assert.assertEquals(1, oneIteration.afterIterations());
	}

	@Test
	public void pingPong() throws IOException {
		Cycle cycle = testCase(input -> {
			return input.equals("A") ? "B" : "A";
		}, "CCC", Cycle.class);
		Assert.assertEquals(Arrays.asList("A", "B"), cycle.getCycle());
	}

	@Test
	public void fourState() throws IOException {
		Cycle cycle = testCase(input -> {
			// @formatter:off
			switch (input) {
			case "A": return "B";
			case "B": return "C";
			case "C": return "D";
			default:  return "A";
			}
			// @formatter:on
		}, "CCC", Cycle.class);
		Assert.assertEquals(Arrays.asList("A", "B", "C", "D"), cycle.getCycle());
	}

	@Test
	public void converging() throws IOException {
		Converge converge = testCase(input -> {
			if (input.isEmpty()) {
				return input;
			} else {
				return input.substring(0, input.length() - 1);
			}
		}, "CCC", Converge.class);
		Assert.assertEquals("", converge.convergesTo());
		Assert.assertEquals(3, converge.afterIterations());
	}

	@Test
	public void diverging() throws IOException {
		Diverge diverge = testCase(input -> {
			return input + " ";
		}, "", Diverge.class);
		Assert.assertEquals(Arrays.asList(" ",
				"  ", "   ", "    ", "     ",
				"      ", "       ", "        ",
				"         ", "          "), diverge.steps());
	}

	@Test
	public void cycleOrder() {
		BiConsumer<String, String> testCase = (unorderedStr, expectedStr) -> {
			List<String> unordered = Arrays.asList(unorderedStr.split(","));
			List<String> expected = Arrays.asList(expectedStr.split(","));
			Result result = Result.cycle(folder.getRoot(), unordered);
			Assert.assertEquals(expected, result.as(Cycle.class).getCycle());
		};
		// alphabetic
		testCase.accept("a,b,c", "a,b,c");
		testCase.accept("b,c,a", "a,b,c");
		testCase.accept("c,a,b", "a,b,c");
		// length
		testCase.accept("a,aa,aaa", "a,aa,aaa");
		testCase.accept("aa,aaa,a", "a,aa,aaa");
		testCase.accept("aaa,a,aa", "a,aa,aaa");
		// length > alphabetic
		testCase.accept("b,aa,aaa", "b,aa,aaa");
		testCase.accept("aa,aaa,b", "b,aa,aaa");
		testCase.accept("aa,b,aaa", "b,aaa,aa");
	}
}
