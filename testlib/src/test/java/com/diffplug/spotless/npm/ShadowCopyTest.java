/*
 * Copyright 2023-2025 DiffPlug
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.Suppliers;
import com.diffplug.spotless.ResourceHarness;

class ShadowCopyTest extends ResourceHarness {

	public static final char[] CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
	private File shadowCopyRoot;

	private ShadowCopy shadowCopy;

	private final Random random = new Random();

	@BeforeEach
	void setUp() throws IOException {
		shadowCopyRoot = newFolder("shadowCopyRoot");
		shadowCopy = new ShadowCopy(Suppliers.ofInstance(shadowCopyRoot));
	}

	@Test
	void anAddedEntryCanBeRetrieved() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		shadowCopy.addEntry("someEntry", folderWithRandomFile);
		File shadowCopyFile = shadowCopy.getEntry("someEntry", folderWithRandomFile.getName());
		Assertions.assertThat(shadowCopyFile.listFiles()).hasSize(folderWithRandomFile.listFiles().length);
		assertAllFilesAreEqualButNotSameAbsolutePath(folderWithRandomFile, shadowCopyFile);
	}

	@Test
	void twoAddedEntriesCanBeRetrieved() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		File folderWithRandomFile2 = newFolderWithRandomFile();
		shadowCopy.addEntry("someEntry", folderWithRandomFile);
		shadowCopy.addEntry("someOtherEntry", folderWithRandomFile2);
		File shadowCopyFile = shadowCopy.getEntry("someEntry", folderWithRandomFile.getName());
		File shadowCopyFile2 = shadowCopy.getEntry("someOtherEntry", folderWithRandomFile2.getName());
		Assertions.assertThat(shadowCopyFile.listFiles()).hasSize(folderWithRandomFile.listFiles().length);
		Assertions.assertThat(shadowCopyFile2.listFiles()).hasSize(folderWithRandomFile2.listFiles().length);
		assertAllFilesAreEqualButNotSameAbsolutePath(folderWithRandomFile, shadowCopyFile);
		assertAllFilesAreEqualButNotSameAbsolutePath(folderWithRandomFile2, shadowCopyFile2);
	}

	@Test
	void addingTheSameEntryTwiceWorks() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		shadowCopy.addEntry("someEntry", folderWithRandomFile);
		shadowCopy.addEntry("someEntry", folderWithRandomFile);
		File shadowCopyFile = shadowCopy.getEntry("someEntry", folderWithRandomFile.getName());
		Assertions.assertThat(shadowCopyFile.listFiles()).hasSize(folderWithRandomFile.listFiles().length);
		assertAllFilesAreEqualButNotSameAbsolutePath(folderWithRandomFile, shadowCopyFile);
	}

	@Test
	void changingAFolderAfterAddingItDoesNotChangeTheShadowCopy() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		shadowCopy.addEntry("someEntry", folderWithRandomFile);

		// now change the orig
		Files.delete(folderWithRandomFile.listFiles()[0].toPath());
		File newRandomFile = new File(folderWithRandomFile, "replacedFile.txt");
		writeRandomStringOfLengthToFile(newRandomFile, 100);

		// now check that they are different
		File shadowCopy = this.shadowCopy.getEntry("someEntry", folderWithRandomFile.getName());
		Assertions.assertThat(shadowCopy.listFiles()).hasSize(folderWithRandomFile.listFiles().length);
		Assertions.assertThat(shadowCopy.listFiles()[0].getName()).isNotEqualTo(folderWithRandomFile.listFiles()[0].getName());
	}

	@Test
	void aFolderCanBeCopiedUsingShadowCopy() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		shadowCopy.addEntry("someEntry", folderWithRandomFile);
		File copiedFolder = newFolder("copyDest");
		File copiedEntry = shadowCopy.copyEntryInto("someEntry", folderWithRandomFile.getName(), copiedFolder);

		Assertions.assertThat(copiedEntry.listFiles()).hasSize(folderWithRandomFile.listFiles().length);
		assertAllFilesAreEqualButNotSameAbsolutePath(folderWithRandomFile, copiedEntry);
	}

	@Test
	void aCopiedFolderIsDifferentFromShadowCopyEntry() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		shadowCopy.addEntry("someEntry", folderWithRandomFile);
		File copiedFolder = newFolder("copyDest");
		File copiedEntry = shadowCopy.copyEntryInto("someEntry", folderWithRandomFile.getName(), copiedFolder);

		File shadowCopyFile = shadowCopy.getEntry("someEntry", folderWithRandomFile.getName());
		Assertions.assertThat(shadowCopyFile.listFiles()).hasSize(copiedEntry.listFiles().length);
		assertAllFilesAreEqualButNotSameAbsolutePath(copiedEntry, shadowCopyFile);
	}

	@Test
	void anAddedEntryExistsAfterAdding() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		shadowCopy.addEntry("someEntry", folderWithRandomFile);
		Assertions.assertThat(shadowCopy.entryExists("someEntry", folderWithRandomFile.getName())).isTrue();
	}

	@Test
	void aEntryThatHasNotBeenAddedDoesNotExist() throws IOException {
		File folderWithRandomFile = newFolderWithRandomFile();
		Assertions.assertThat(shadowCopy.entryExists("someEntry", folderWithRandomFile.getName())).isFalse();
	}

	private void assertAllFilesAreEqualButNotSameAbsolutePath(File expected, File actual) {
		if (expected.isFile()) {
			assertFileIsEqualButNotSameAbsolutePath(expected, actual);
		} else {
			assertDirectoryIsEqualButNotSameAbsolutePath(expected, actual);
		}
	}

	private void assertDirectoryIsEqualButNotSameAbsolutePath(File expected, File actual) {
		Assertions.assertThat(actual.getAbsolutePath()).as("absolute path should be different").isNotEqualTo(expected.getAbsolutePath());
		Assertions.assertThat(actual.listFiles()).as("folder should have same amount of files").hasSize(expected.listFiles().length);
		List<File> actualContent = filesInAlphabeticalOrder(actual);
		List<File> expectedContent = filesInAlphabeticalOrder(expected);

		for (int i = 0; i < expectedContent.size(); i++) {
			assertAllFilesAreEqualButNotSameAbsolutePath(expectedContent.get(i), actualContent.get(i));
		}
	}

	private List<File> filesInAlphabeticalOrder(File folder) {
		if (!folder.isDirectory()) {
			throw new IllegalArgumentException("folder must be a directory");
		}
		return Arrays.stream(folder.listFiles())
				.sorted(Comparator.comparing(File::getName).thenComparing(File::getAbsolutePath))
				.collect(Collectors.toList());
	}

	private void assertFileIsEqualButNotSameAbsolutePath(File expected, File actual) {
		Assertions.assertThat(actual).as("Files have same name").hasName(expected.getName());
		Assertions.assertThat(actual.getAbsolutePath()).as("absolute path is different").isNotEqualTo(expected.getAbsolutePath());
		Assertions.assertThat(actual).as("files have same content").hasSameTextualContentAs(expected, StandardCharsets.UTF_8);
	}

	private File newFolderWithRandomFile() throws IOException {
		File folder = newFolder(randomStringOfLength(10));
		File file = new File(folder, randomStringOfLength(10) + ".txt");
		writeRandomStringOfLengthToFile(file, 10);
		return folder;
	}

	private void writeRandomStringOfLengthToFile(File file, int length) throws IOException {
		Files.write(file.toPath(), randomStringOfLength(length).getBytes(StandardCharsets.UTF_8));
	}

	private String randomStringOfLength(int length) {
		// returns a string of length containing characters a-z, A-Z, 0-9

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < length; i++) {
			sb.append(CHARS[random.nextInt(CHARS.length)]);
		}
		return sb.toString();
	}

}
