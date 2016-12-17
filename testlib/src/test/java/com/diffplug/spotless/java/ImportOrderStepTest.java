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
package com.diffplug.spotless.java;

import java.util.Arrays;

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.NonSerializableList;
import com.diffplug.spotless.ResourceHarness;

public class ImportOrderStepTest extends ResourceHarness {
	@Test
	public void sortImportsFromArray() throws Throwable {
		FormatterStep step = ImportOrderStep.createFromOrder(Arrays.asList("java", "javax", "org", "\\#com"));
		assertOnResources(step, "java/eclipse/importsorter/JavaCodeUnsortedImports.test", "java/eclipse/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsFromFile() throws Throwable {
		FormatterStep step = ImportOrderStep.createFromFile(createTestFile("java/eclipse/importsorter/import.properties"));
		assertOnResources(step, "java/eclipse/importsorter/JavaCodeUnsortedImports.test", "java/eclipse/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsUnmatched() throws Throwable {
		FormatterStep step = ImportOrderStep.createFromFile(createTestFile("java/eclipse/importsorter/import_unmatched.properties"));
		assertOnResources(step, "java/eclipse/importsorter/JavaCodeUnsortedImportsUnmatched.test", "java/eclipse/importsorter/JavaCodeSortedImportsUnmatched.test");
	}

	@Test
	public void removeDuplicates() throws Throwable {
		FormatterStep step = ImportOrderStep.createFromFile(createTestFile("java/eclipse/importsorter/import_unmatched.properties"));
		assertOnResources(step, "java/eclipse/importsorter/JavaCodeSortedDuplicateImportsUnmatched.test", "java/eclipse/importsorter/JavaCodeSortedImportsUnmatched.test");
	}

	@Test
	public void doesntThrowIfImportOrderIsntSerializable() {
		ImportOrderStep.createFromOrder(NonSerializableList.of("java", "javax", "org", "\\#com"));
	}
}
