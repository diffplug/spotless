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

import static com.diffplug.gradle.spotless.PaddedCell.ResultType.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Throwing;

public class PaddedCellTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private void misbehaved(Throwing.Function<String, String> step, String input, PaddedCell.ResultType expectedOutputType, String steps) throws IOException {
		testCase(step, input, expectedOutputType, steps, true);
	}

	private void wellbehaved(Throwing.Function<String, String> step, String input, PaddedCell.ResultType expectedOutputType, String steps) throws IOException {
		testCase(step, input, expectedOutputType, steps, false);
	}

	private void testCase(Throwing.Function<String, String> step, String input, PaddedCell.ResultType expectedOutputType, String expectedSteps, boolean misbehaved) throws IOException {
		List<FormatterStep> formatterSteps = new ArrayList<>();
		formatterSteps.add(FormatterStep.create("step", step));
		Formatter formatter = new Formatter(LineEnding.UNIX_POLICY, folder.getRoot().toPath(), formatterSteps);

		File file = folder.newFile();
		Files.write(file.toPath(), input.getBytes(StandardCharsets.UTF_8));

		PaddedCell result = PaddedCell.check(formatter, file);
		Assert.assertEquals(misbehaved, result.misbehaved());
		Assert.assertEquals(expectedOutputType, result.type());

		String actual = result.steps().stream().collect(Collectors.joining(","));
		Assert.assertEquals(expectedSteps, actual);
	}

	@Test
	public void wellBehaved() throws IOException {
		wellbehaved(input -> input, "CCC", CONVERGE, "CCC");
		wellbehaved(input -> "A", "CCC", CONVERGE, "A");
	}

	@Test
	public void pingPong() throws IOException {
		misbehaved(input -> {
			return input.equals("A") ? "B" : "A";
		}, "CCC", CYCLE, "A,B");
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
		}, "CCC", CYCLE, "A,B,C,D");
	}

	@Test
	public void converging() throws IOException {
		misbehaved(input -> {
			if (input.isEmpty()) {
				return input;
			} else {
				return input.substring(0, input.length() - 1);
			}
		}, "CCC", CONVERGE, "CC,C,");
	}

	@Test
	public void diverging() throws IOException {
		misbehaved(input -> {
			return input + " ";
		}, "", DIVERGE, " ,  ,   ,    ,     ,      ,       ,        ,         ,          ");
	}

	@Test
	public void cycleOrder() {
		BiConsumer<String, String> testCase = (unorderedStr, expectedStr) -> {
			List<String> unordered = Arrays.asList(unorderedStr.split(","));
			PaddedCell result = PaddedCell.cycle(folder.getRoot(), unordered);
			String resultJoined = result.steps().stream().collect(Collectors.joining(","));
			Assert.assertEquals(expectedStr, resultJoined);
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
