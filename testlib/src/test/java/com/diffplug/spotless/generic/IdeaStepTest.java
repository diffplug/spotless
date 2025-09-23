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

	private File buildDir = null;

	protected File buildDir() {
		if (this.buildDir == null) {
			this.buildDir = ThrowingEx.get(() -> newFolder("build-dir"));
		}
		return this.buildDir;
	}
}
