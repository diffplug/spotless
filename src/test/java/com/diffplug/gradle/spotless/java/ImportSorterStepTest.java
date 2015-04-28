package com.diffplug.gradle.spotless.java;

import java.util.Arrays;

import org.junit.Test;

import com.diffplug.gradle.spotless.ResourceTest;

public class ImportSorterStepTest extends ResourceTest {
	@Test
	public void sortImportsFromArray() throws Throwable {
		ImportSorterStep step = new ImportSorterStep(Arrays.asList("java", "javax", "org", "\\#com"));
		assertStep(step::format, "JavaCodeUnsortedImports.test", "JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsFromFile() throws Throwable {
		ImportSorterStep step = new ImportSorterStep(createTestFile("import.properties"));
		assertStep(step::format, "JavaCodeUnsortedImports.test", "JavaCodeSortedImports.test");
	}
}
