/*
 * Copyright 2021 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexBasedCheckerTest extends FileIndexHarness {

	private FileIndex index;
	private IndexBasedChecker checker;

	@BeforeEach
	void beforeEach() {
		index = FileIndex.read(config, log);
		checker = new IndexBasedChecker(index, log);
	}

	@Test
	void isUpToDateReturnsFalseForUnknownFile() throws Exception {
		Path sourceFile = createSourceFile("source.txt");
		assertThat(checker.isUpToDate(sourceFile)).isFalse();
	}

	@Test
	void isUpToDateReturnsTrueWhenOnDiskFileIsSameAsInTheIndex() throws Exception {
		Path sourceFile = createSourceFile("source.txt");
		Instant modifiedTime = Files.getLastModifiedTime(sourceFile).toInstant();
		index.setLastModifiedTime(sourceFile, modifiedTime);

		assertThat(checker.isUpToDate(sourceFile)).isTrue();
	}

	@Test
	void isUpToDateReturnsFalseWhenOnDiskFileIsNewerThanInTheIndex() throws Exception {
		Path sourceFile = createSourceFile("source.txt");
		Instant modifiedTime = Files.getLastModifiedTime(sourceFile).toInstant().minusSeconds(42);
		index.setLastModifiedTime(sourceFile, modifiedTime);

		assertThat(checker.isUpToDate(sourceFile)).isFalse();
	}

	/**
	 * This test checks a bit of a weird case when file's last modified time in the index is greater
	 * than file's last modified time on-disk. This should not happen because of how the index is
	 * used. To be on the safe side, we consider the file to be out of date if this ever happens.
	 */
	@Test
	void isUpToDateReturnsFalseWhenOnDiskFileIsOlderThanInTheIndex() throws Exception {
		Path sourceFile = createSourceFile("source.txt");
		Instant modifiedTime = Files.getLastModifiedTime(sourceFile).toInstant().plusSeconds(42);
		index.setLastModifiedTime(sourceFile, modifiedTime);

		assertThat(checker.isUpToDate(sourceFile)).isFalse();
	}

	@Test
	void setUpToDateUpdatesTheIndex() throws Exception {
		Path sourceFile = createSourceFile("source.txt");
		assertThat(index.getLastModifiedTime(sourceFile)).isNull();
		assertThat(checker.isUpToDate(sourceFile)).isFalse();

		checker.setUpToDate(sourceFile);

		assertThat(index.getLastModifiedTime(sourceFile)).isEqualTo(Files.getLastModifiedTime(sourceFile).toInstant());
		assertThat(checker.isUpToDate(sourceFile)).isTrue();
	}

	@Test
	void closeWritesTheIndex() throws Exception {
		Path sourceFile = createSourceFile("source.txt");
		assertThat(index.getLastModifiedTime(sourceFile)).isNull();

		checker.setUpToDate(sourceFile);
		checker.close();

		FileIndex newIndex = FileIndex.read(config, log);
		assertThat(newIndex.getLastModifiedTime(sourceFile)).isEqualTo(Files.getLastModifiedTime(sourceFile).toInstant());
	}
}
