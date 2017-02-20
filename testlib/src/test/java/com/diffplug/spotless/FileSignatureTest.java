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
package com.diffplug.spotless;

import static com.diffplug.spotless.FileSignature.Ignore;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class FileSignatureTest extends ResourceHarness {

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		List<Object[]> io = new LinkedList<Object[]>();
		io.addAll(
				test(EnumSet.noneOf(Ignore.class))
						.input()
						.expected()
						.input("A")
						.expected("A")
						.input("B", "A", "A")
						.expected("B", "A", "A")
						.asList());
		io.addAll(
				test(Ignore.ORDER)
						.input()
						.expected()
						.input("A")
						.expected("A")
						.input("C", "A", "A", "B")
						.expected("A", "A", "B", "C")
						.asList());
		io.addAll(
				test(Ignore.NEXT_DUPLICATES)
						.input()
						.expected()
						.input("A")
						.expected("A")
						.input("A", "B", "A")
						.expected("A", "B")
						.input("A", "C", "A", "B")
						.expected("A", "C", "B")
						.asList());
		io.addAll(
				test(Ignore.PREVIOUS_DUPLICATES)
						.input()
						.expected()
						.input("A")
						.expected("A")
						.input("A", "B", "A")
						.expected("B", "A")
						.input("B", "A", "C", "A")
						.expected("B", "C", "A")
						.asList());
		io.addAll(
				test(Ignore.ORDER_AND_DUPLICATES)
						.input()
						.expected()
						.input("A")
						.expected("A")
						.input("A", "C", "C", "A", "B")
						.expected("A", "B", "C")
						.asList());
		return io;
	}

	private static TestData test(final Ignore ignore) {
		return new TestData(EnumSet.of(ignore));
	}

	private static TestData test(EnumSet<Ignore> ignores) {
		return new TestData(ignores);
	}

	private static class TestData {
		private final EnumSet<Ignore> ignores;
		private final List<String[]> inputPaths;
		private final List<String[]> expectedPaths;

		public TestData(final EnumSet<Ignore> ignores) {
			this.ignores = ignores;
			this.inputPaths = new LinkedList<String[]>();
			this.expectedPaths = new LinkedList<String[]>();
		}

		public TestData input(String... filePaths) {
			this.inputPaths.add(filePaths);
			return this;
		}

		public TestData expected(String... filePaths) {
			this.expectedPaths.add(filePaths);
			return this;
		}

		public List<Object[]> asList() {
			if (inputPaths.size() != expectedPaths.size()) {
				String msg = String.format("Unequal number of inputs (%d) and expected outputs (%d).",
						inputPaths.size(),
						expectedPaths.size());
				throw new IllegalArgumentException(msg);
			}
			List<Object[]> result = new ArrayList<Object[]>(inputPaths.size());
			Iterator<String[]> inputPathsItr = inputPaths.iterator();
			Iterator<String[]> expectedPathsItr = expectedPaths.iterator();
			while (inputPathsItr.hasNext()) {
				String[] inputPaths = inputPathsItr.next();
				List<String> inputPathList = Arrays.asList(inputPaths);
				String testName = String.format("Files %s, Ignores %s", inputPathList, ignores);
				result.add(new Object[]{
						testName,
						ignores,
						inputPaths,
						expectedPathsItr.next()
				});
			}
			return result;
		}

	}

	@Parameter()
	public String testCaseNameNotUsed;

	@Parameter(value = 1)
	public EnumSet<Ignore> ignores;

	@Parameter(value = 2)
	public String[] inputPaths;

	@Parameter(value = 3)
	public String[] expectedPaths;

	@Test
	public void testCreate() throws IOException {
		Collection<File> inputFiles = getTestFiles(inputPaths);
		FileSignature signature = FileSignature.from(inputFiles, ignores);
		Collection<File> expectedFiles = getTestFiles(expectedPaths);
		Collection<File> outputFiles = signature.files();
		assertThat(outputFiles).containsExactlyElementsOf(expectedFiles);
	}

	private List<File> getTestFiles(final String[] paths) throws IOException {
		final List<File> result = new ArrayList<File>(paths.length);
		for (String path : paths) {
			result.add(createTestFile(path, ""));
		}
		return result;
	}

}
