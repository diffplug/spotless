/*
 * Copyright 2016-2021 DiffPlug
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
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

	@Test
	public void testFromDirectory() {
		File dir = new File(rootFolder(), "dir");
		assertThatThrownBy(() -> FileSignature.signAsList(dir))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testFromFilesAndDirectory() throws IOException {
		File dir = new File(rootFolder(), "dir");
		List<File> files = getTestFiles(inputPaths);
		files.add(dir);
		Collections.shuffle(files);
		assertThatThrownBy(() -> FileSignature.signAsList(files))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private List<File> getTestFiles(final String[] paths) throws IOException {
		final List<File> result = new ArrayList<>(paths.length);
		for (String path : paths) {
			result.add(setFile(path).toContent(""));
		}
		return result;
	}

	@Test
	public void testSubpath() {
		assertThat(FileSignature.subpath("root/", "root/child")).isEqualTo("child");
	}

	@Test
	public void windowsRoot() {
		org.junit.Assume.assumeTrue(FileSignature.machineIsWin());
		String subpath = FileSignature.subpath("S://", "S:/build.gradle");
		Assertions.assertThat(subpath).isEqualTo("build.gradle");
	}
}
