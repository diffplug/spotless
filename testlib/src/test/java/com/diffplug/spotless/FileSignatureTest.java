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

import static java.util.Collections.shuffle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.condition.OS.WINDOWS;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

class FileSignatureTest extends ResourceHarness {
	private static final String[] INPUT_PATHS = {"A", "C", "C", "A", "B"};
	private static final String[] EXPECTED_PATH_LIST = INPUT_PATHS;
	private static final String[] EXPECTED_PATH_SET = {"A", "B", "C"};

	@Test
	void testFromList() throws IOException {
		Collection<File> inputFiles = getTestFiles(INPUT_PATHS);
		FileSignature signature = FileSignature.signAsList(inputFiles);
		Collection<File> expectedFiles = getTestFiles(EXPECTED_PATH_LIST);
		Collection<File> outputFiles = signature.files();
		assertThat(outputFiles).containsExactlyElementsOf(expectedFiles);
	}

	@Test
	void testFromSet() throws IOException {
		Collection<File> inputFiles = getTestFiles(INPUT_PATHS);
		FileSignature signature = FileSignature.signAsSet(inputFiles);
		Collection<File> expectedFiles = getTestFiles(EXPECTED_PATH_SET);
		Collection<File> outputFiles = signature.files();
		assertThat(outputFiles).containsExactlyElementsOf(expectedFiles);
	}

	@Test
	void testFromDirectory() {
		File dir = new File(rootFolder(), "dir");
		assertThatThrownBy(() -> FileSignature.signAsList(dir))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testFromFilesAndDirectory() throws IOException {
		File dir = new File(rootFolder(), "dir");
		List<File> files = getTestFiles(INPUT_PATHS);
		files.add(dir);
		shuffle(files);
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
	void testSubpath() {
		assertThat(FileSignature.subpath("root/", "root/child")).isEqualTo("child");
	}

	@Test
	@EnabledOnOs(WINDOWS)
	void windowsRoot() {
		String subpath = FileSignature.subpath("S://", "S:/build.gradle");
		assertThat(subpath).isEqualTo("build.gradle");
	}
}
