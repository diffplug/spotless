/*
 * Copyright 2026 DiffPlug
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
package com.diffplug.spotless.npm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileFinderTest {

	@TempDir
	Path tempDir;

	@Test
	void directoryWithExecutableNameIsNotAnExecutable() throws IOException {
		Files.createDirectory(tempDir.resolve("npm"));

		FileFinder finder = FileFinder.finderForExecutableFilename("npm")
				.candidateFileInFolder(tempDir.toFile())
				.build();

		assertThat(finder.tryFind()).isEmpty();
	}

	@Test
	void executableFileIsFound() throws IOException {
		Path npm = Files.createFile(tempDir.resolve("npm"));
		assertThat(npm.toFile().setExecutable(true)).isTrue();

		FileFinder finder = FileFinder.finderForExecutableFilename("npm")
				.candidateFileInFolder(tempDir.toFile())
				.build();

		assertThat(finder.tryFind()).contains(new File(tempDir.toFile(), "npm"));
	}
}
