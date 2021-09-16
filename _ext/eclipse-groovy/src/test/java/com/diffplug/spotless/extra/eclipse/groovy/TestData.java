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
package com.diffplug.spotless.extra.eclipse.groovy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestData {
	public static TestData getTestDataOnFileSystem() {
		final String userDir = System.getProperty("user.dir", ".");
		Path dataPath = Paths.get(userDir, "src", "test", "resources");
		if (Files.isDirectory(dataPath)) {
			return new TestData(dataPath);
		}
		return null;
	}

	private final Path inputPath;
	private final Path expectedPath;

	private TestData(Path dataPath) {
		inputPath = dataPath.resolve("input").toAbsolutePath();
		expectedPath = dataPath.resolve("expected").toAbsolutePath();
		for (Path testDataDir : new Path[]{inputPath, expectedPath}) {
			if (!Files.isDirectory(testDataDir)) {
				throw new IllegalArgumentException(String.format("'%1$s' is not a directory.", testDataDir));
			}
		}
	}

	public String input(final String fileName) throws Exception {
		return read(inputPath.resolve(fileName));
	}

	public String expected(final String fileName) {
		Path path = expectedPath.resolve(fileName);
		return read(path);
	}

	private String read(final Path xmlPath) {
		if (!Files.isRegularFile(xmlPath)) {
			throw new IllegalArgumentException(String.format("'%1$s' is not a regular file.", xmlPath));
		}
		try {
			String checkedOutFileContent = new String(java.nio.file.Files.readAllBytes(xmlPath), "UTF8");
			return checkedOutFileContent.replace("\r", ""); //Align GIT end-of-line normalization
		} catch (IOException e) {
			throw new IllegalArgumentException(String.format("Failed to read '%1$s'.", xmlPath), e);
		}
	}

}
