/*
 * Copyright 2024-2025 DiffPlug
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
package com.diffplug.spotless.generic;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diffplug.common.io.Files;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.tag.IdeaTest;

@IdeaTest
class IdeaStepTest extends ResourceHarness {

	@Test
	void name() throws Exception {
		FormatterStep step = IdeaStep.newBuilder(buildDir()).setUseDefaults(true).build();

		String name = step.getName();

		Assertions.assertEquals("IDEA", name);
	}

	@Test
	void notFormattings() throws Exception {
		File cleanFile = newFile("clean.java");
		String cleanJava = ResourceHarness.getTestResource("java/idea/full.clean.java");
		Files.write(cleanJava, cleanFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.newBuilder(buildDir()).setUseDefaults(true).build();

		var result = step.format(cleanJava, cleanFile);

		Assertions.assertEquals(cleanJava, result,
				"formatting was applied to clean file");
	}

	@Test
	void formattings() throws Exception {
		File dirtyFile = newFile("dirty.java");
		String dirtyJava = ResourceHarness.getTestResource("java/idea/full.dirty.java");
		Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.newBuilder(buildDir()).setUseDefaults(true).build();

		var result = step.format(dirtyJava, dirtyFile);

		Assertions.assertNotEquals(dirtyJava, result,
				"files were not changed after reformat");
	}

	@Test
	void formattingsWorkWithDefaultParameters() throws Exception {
		File dirtyFile = newFile("dirty.java");
		String dirtyJava = ResourceHarness.getTestResource("java/idea/full.dirty.java");
		Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.newBuilder(buildDir()).build();

		var result = step.format(dirtyJava, dirtyFile);

		Assertions.assertNotEquals(dirtyJava, result,
				"files were not changed after reformat");
	}

	@Test
	void formattingsWithoutDefaultDoesNothing() throws Exception {
		File dirtyFile = newFile("dirty.java");
		String dirtyJava = ResourceHarness.getTestResource("java/idea/full.dirty.java");
		Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.newBuilder(buildDir()).setUseDefaults(false).build();

		var result = step.format(dirtyJava, dirtyFile);

		Assertions.assertEquals(dirtyJava, result,
				"files were changed after reformat");
	}

	@Test
	void configureFile() throws Exception {
		File cleanFile = newFile("clean.java");
		String cleanJava = ResourceHarness.getTestResource("java/idea/full.clean.java");
		Files.write(cleanJava, cleanFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.newBuilder(buildDir()).setUseDefaults(true).build();

		var result = step.format(cleanJava, cleanFile);

		Assertions.assertEquals(cleanJava, result,
				"formatting was applied to clean file");
	}

	@Test
	void batchFormattingSingleBatch() throws Exception {
		// Test formatting multiple files in a single batch
		String dirtyJava = ResourceHarness.getTestResource("java/idea/full.dirty.java");
		FormatterStep step = IdeaStep.newBuilder(buildDir())
				.setUseDefaults(true)
				.setBatchSize(5) // Batch size larger than number of files
				.build();

		// Format 3 files - should all be in one batch
		for (int i = 0; i < 3; i++) {
			File dirtyFile = newFile("dirty" + i + ".java");
			Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
			var result = step.format(dirtyJava, dirtyFile);
			Assertions.assertNotEquals(dirtyJava, result,
					"file " + i + " was not formatted");
		}
	}

	@Test
	void batchFormattingMultipleBatches() throws Exception {
		// Test formatting files across exactly 3 batches
		String dirtyJava = ResourceHarness.getTestResource("java/idea/full.dirty.java");
		FormatterStep step = IdeaStep.newBuilder(buildDir())
				.setUseDefaults(true)
				.setBatchSize(2) // Small batch size to force multiple batches
				.build();

		// Format 6 files - should be exactly 3 batches (2+2+2)
		for (int i = 0; i < 6; i++) {
			File dirtyFile = newFile("batch_dirty" + i + ".java");
			Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
			var result = step.format(dirtyJava, dirtyFile);
			Assertions.assertNotEquals(dirtyJava, result,
					"file " + i + " was not formatted in batch");
		}
	}

	@Test
	void batchFormattingPartialLastBatch() throws Exception {
		// Test formatting files with a partial last batch
		String dirtyJava = ResourceHarness.getTestResource("java/idea/full.dirty.java");
		FormatterStep step = IdeaStep.newBuilder(buildDir())
				.setUseDefaults(true)
				.setBatchSize(2) // Small batch size
				.build();

		// Format 7 files - should be 4 batches (2+2+2+1)
		for (int i = 0; i < 7; i++) {
			File dirtyFile = newFile("partial_dirty" + i + ".java");
			Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
			var result = step.format(dirtyJava, dirtyFile);
			Assertions.assertNotEquals(dirtyJava, result,
					"file " + i + " was not formatted with partial batch");
		}
	}

	@Test
	void batchFormattingMixedCleanAndDirty() throws Exception {
		// Test formatting a mix of clean and dirty files across multiple batches
		String dirtyJava = ResourceHarness.getTestResource("java/idea/full.dirty.java");
		String cleanJava = ResourceHarness.getTestResource("java/idea/full.clean.java");
		FormatterStep step = IdeaStep.newBuilder(buildDir())
				.setUseDefaults(true)
				.setBatchSize(3)
				.build();

		// Format 9 files (3 batches) - alternating dirty and clean
		for (int i = 0; i < 9; i++) {
			boolean isDirty = i % 2 == 0;
			String content = isDirty ? dirtyJava : cleanJava;
			File file = newFile("mixed" + i + ".java");
			Files.write(content, file, StandardCharsets.UTF_8);
			var result = step.format(content, file);

			if (isDirty) {
				Assertions.assertNotEquals(content, result,
						"dirty file " + i + " was not formatted");
			} else {
				Assertions.assertEquals(content, result,
						"clean file " + i + " was incorrectly modified");
			}
		}
	}

	@Test
	void batchSizeValidation() {
		// Test that batch size must be at least 1
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			IdeaStep.newBuilder(buildDir())
					.setUseDefaults(true)
					.setBatchSize(0)
					.build();
		}, "batch size of 0 should throw exception");

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			IdeaStep.newBuilder(buildDir())
					.setUseDefaults(true)
					.setBatchSize(-1)
					.build();
		}, "negative batch size should throw exception");
	}

	private File buildDir;

	protected File buildDir() {
		if (this.buildDir == null) {
			this.buildDir = ThrowingEx.get(() -> newFolder("build-dir"));
		}
		return this.buildDir;
	}
}
