/*
 * Copyright 2016-2019 DiffPlug
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

import org.junit.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;

public class ImportOrderStepTest extends ResourceHarness {

	@Test
	public void sortImportsFromArray() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom("java", "javax", "org", "\\#com");
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImports.test", "java/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsFromFile() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImports.test", "java/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	public void sortImportsUnmatched() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import_unmatched.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImportsUnmatched.test", "java/importsorter/JavaCodeSortedImportsUnmatched.test");
	}

	@Test
	public void removeDuplicates() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import_unmatched.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeSortedDuplicateImportsUnmatched.test", "java/importsorter/JavaCodeSortedImportsUnmatched.test");
	}

	@Test
	public void removeComments() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeImportComments.test", "java/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	public void misplacedImports() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedMisplacedImports.test", "java/importsorter/JavaCodeSortedMisplacedImports.test");
	}

	@Test
	public void empty() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeEmptyFile.test", "java/importsorter/JavaCodeEmptyFile.test");
	}

	@Test
	public void groovyImports() throws Throwable {
		FormatterStep step = ImportOrderStep.forGroovy().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/GroovyCodeUnsortedMisplacedImports.test", "java/importsorter/GroovyCodeSortedMisplacedImports.test");
	}

	@Test
	public void doesntThrowIfImportOrderIsntSerializable() {
		ImportOrderStep.forJava().createFrom("java", "javax", "org", "\\#com");
	}

	@Test
	public void equality() throws Exception {
		new SerializableEqualityTester() {
			String[] imports = {};

			@Override
			protected void setupTest(API api) {
				// same version == same
				api.areDifferentThan();
				// change the version, and it's different
				imports = new String[]{"a"};
				api.areDifferentThan();
				// change the version, and it's different
				imports = new String[]{"b"};
				api.areDifferentThan();
				// change the version, and it's different
				imports = new String[]{"a", "b"};
				api.areDifferentThan();
			}

			@Override
			protected FormatterStep create() {
				return ImportOrderStep.forJava().createFrom(imports);
			}
		}.testEquals();
	}

}
