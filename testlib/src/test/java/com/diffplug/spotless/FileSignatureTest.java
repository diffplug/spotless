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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

public class FileSignatureTest extends ResourceHarness {

	private final static String[] inputPaths = {"A", "C", "C", "A", "B"};
	private final static String[] expectedPathList = inputPaths;
	private final static String[] expectedPathSet = {"A", "B", "C"};

	@Test
	public void testFromList() throws IOException {
		Collection<File> inputFiles = getTestFiles(inputPaths);
		FileSignature signature = FileSignature.signAsList(inputFiles);
		Collection<File> expectedFiles = getTestFiles(expectedPathList);
		Collection<File> outputFiles = signature.files();
		assertThat(outputFiles).containsExactlyElementsOf(expectedFiles);
	}

	@Test
	public void testFromSet() throws IOException {
		Collection<File> inputFiles = getTestFiles(inputPaths);
		FileSignature signature = FileSignature.signAsSet(inputFiles);
		Collection<File> expectedFiles = getTestFiles(expectedPathSet);
		Collection<File> outputFiles = signature.files();
		assertThat(outputFiles).containsExactlyElementsOf(expectedFiles);
	}

	private List<File> getTestFiles(final String[] paths) throws IOException {
		final List<File> result = new ArrayList<>(paths.length);
		for (String path : paths) {
			result.add(createTestFile(path, ""));
		}
		return result;
	}

}
