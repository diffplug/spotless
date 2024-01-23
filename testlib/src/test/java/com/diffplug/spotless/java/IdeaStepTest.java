package com.diffplug.spotless.java;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.diffplug.common.io.Files;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;

class IdeaStepTest extends ResourceHarness {

	@Test
	void name() throws Exception {
		FormatterStep step = IdeaStep.create(true, "idea");

		String name = step.getName();

		Assertions.assertEquals("IDEA", name);
	}

	@Test
	void notFormattings() throws Exception {
		File cleanFile = newFile("clean.java");
		String cleanJava =
				ResourceHarness.getTestResource("java/idea/full.clean.java");
		Files.write(cleanJava, cleanFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.create(true, "idea");

		var result = step.format(cleanJava, cleanFile);

		Assertions.assertEquals(cleanJava, result,
				"formatting was applied to clean file");
	}

	@Test
	void formattings() throws Exception {
		File dirtyFile = newFile("dirty.java");
		String dirtyJava =
				ResourceHarness.getTestResource("java/idea/full.dirty.java");
		Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.create(true, "idea");

		var result = step.format(dirtyJava, dirtyFile);

		Assertions.assertNotEquals(dirtyJava, result,
				"files were not changed after reformat");
	}

	@Test
	void formattingsWorkWithDefaultParameters() throws Exception {
		File dirtyFile = newFile("dirty.java");
		String dirtyJava =
				ResourceHarness.getTestResource("java/idea/full.dirty.java");
		Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.create();

		var result = step.format(dirtyJava, dirtyFile);

		Assertions.assertNotEquals(dirtyJava, result,
				"files were not changed after reformat");
	}

	@Test
	void formattingsNotDeafaultDoesNothing() throws Exception {
		File dirtyFile = newFile("dirty.java");
		String dirtyJava =
				ResourceHarness.getTestResource("java/idea/full.dirty.java");
		Files.write(dirtyJava, dirtyFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.create(false, "idea");

		var result = step.format(dirtyJava, dirtyFile);

		Assertions.assertEquals(dirtyJava, result,
				"files were changed after reformat");
	}

	@Test
	void configureFile() throws Exception {
		File cleanFile = newFile("clean.java");
		String cleanJava =
				ResourceHarness.getTestResource("java/idea/full.clean.java");
		Files.write(cleanJava, cleanFile, StandardCharsets.UTF_8);
		FormatterStep step = IdeaStep.create(true, "idea");

		var result = step.format(cleanJava, cleanFile);

		Assertions.assertEquals(cleanJava, result,
				"formatting was applied to clean file");
	}
}
