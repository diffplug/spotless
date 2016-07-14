/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.gradle.spotless.java;

import java.util.Arrays;

import org.junit.Test;

import com.diffplug.gradle.spotless.ResourceHarness;

public class ImportSorterStepTest extends ResourceHarness {
	@Test
	public void sortImportsFromArray() throws Throwable {
		ImportSorterStep step = new ImportSorterStep(Arrays.asList("java", "javax", "org", "\\#com"));
		assertStep(step::format, "java/eclipse/importsorter/JavaCodeUnsortedImports.test", "java/eclipse/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsFromFile() throws Throwable {
		ImportSorterStep step = new ImportSorterStep(createTestFile("java/eclipse/importsorter/import.properties"));
		assertStep(step::format, "java/eclipse/importsorter/JavaCodeUnsortedImports.test", "java/eclipse/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsUnmatched() throws Throwable {
		ImportSorterStep step = new ImportSorterStep(createTestFile("java/eclipse/importsorter/import_unmatched.properties"));
		assertStep(step::format, "java/eclipse/importsorter/JavaCodeUnsortedImportsUnmatched.test", "java/eclipse/importsorter/JavaCodeSortedImportsUnmatched.test");
	}
}
