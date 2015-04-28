package com.diffplug.gradle.spotless.java;

import java.util.Arrays;

import org.junit.Test;

import com.diffplug.gradle.spotless.ResourceTest;

public class ImportSorterStepTest extends ResourceTest {
	@Test
	public void sortImportsFromArray() throws Exception {
		ImportSorterStep step = ImportSorterStep.load(Arrays.asList("java", "javax", "org", "\\#com"), null).get();
		assertStep(step, "JavaCodeUnsortedImports.test", "JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsFromFile() throws Exception {
		ImportSorterStep step = ImportSorterStep.load(null, getTestFile("import.properties")).get();
		assertStep(step, "JavaCodeUnsortedImports.test", "JavaCodeSortedImports.test");
	}
}
