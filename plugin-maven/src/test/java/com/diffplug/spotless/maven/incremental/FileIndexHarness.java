/*
 * Copyright 2021-2025 DiffPlug
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
package com.diffplug.spotless.maven.incremental;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

abstract class FileIndexHarness {

	protected static final PluginFingerprint FINGERPRINT = PluginFingerprint.from("foo");

	protected final FileIndexConfig config = mock();
	protected final Log log = mock();

	protected Path tempDir;

	@BeforeEach
	void beforeEach(@TempDir Path tempDir) throws Exception {
		this.tempDir = tempDir;

		Path projectDir = tempDir.resolve("my-project");
		Files.createDirectory(projectDir);
		when(config.getProjectDir()).thenReturn(projectDir);

		Path indexFile = projectDir.resolve("target").resolve("spotless-index");
		when(config.getIndexFile()).thenReturn(indexFile);

		when(config.getPluginFingerprint()).thenReturn(FINGERPRINT);
	}

	protected List<Path> createSourceFilesAndWriteIndexFile(PluginFingerprint fingerprint, String... files) throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add(fingerprint.value());

		List<Path> sourceFiles = new ArrayList<>();
		for (String file : files) {
			Path path = createSourceFile(file);
			lines.add(file + " " + Files.getLastModifiedTime(path).toInstant());
			sourceFiles.add(path);
		}

		writeIndexFile(lines.toArray(new String[0]));
		return sourceFiles;
	}

	protected void writeIndexFile(String... lines) throws IOException {
		Files.createDirectory(config.getIndexFile().getParent());
		Files.createFile(config.getIndexFile());
		Files.write(config.getIndexFile(), Arrays.asList(lines), UTF_8, APPEND);
	}

	protected Path createSourceFile(String name) throws IOException {
		Path file = config.getProjectDir().resolve(name);
		Files.createFile(file);
		return file;
	}
}
