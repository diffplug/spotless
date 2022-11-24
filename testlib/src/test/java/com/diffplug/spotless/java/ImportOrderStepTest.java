/*
 * Copyright 2016-2022 DiffPlug
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

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializableEqualityTester;

class ImportOrderStepTest extends ResourceHarness {
	@Test
	void sortImportsDefault() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom();
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImports.test", "java/importsorter/JavaCodeSortedImportsDefault.test");
	}

	@Test
	void sortImportsFromArray() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom("java", "javax", "org", "\\#com");
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImports.test", "java/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	void sortImportsFromArrayWithSubgroups() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom("java|javax", "org|\\#com", "\\#");
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImportsSubgroups.test", "java/importsorter/JavaCodeSortedImportsSubgroups.test");
	}

	@Test
	void sortImportsFromFile() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImports.test", "java/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	void sortImportsUnmatched() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import_unmatched.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImportsUnmatched.test", "java/importsorter/JavaCodeSortedImportsUnmatched.test");
	}

	@Test
	void sortImportsWildcardsLast() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(true);
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedImports.test", "java/importsorter/JavaCodeSortedImportsWildcardsLast.test");
	}

	@Test
	void removeDuplicates() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import_unmatched.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeSortedDuplicateImportsUnmatched.test", "java/importsorter/JavaCodeSortedImportsUnmatched.test");
	}

	@Test
	void removeComments() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeImportComments.test", "java/importsorter/JavaCodeSortedImports.test");
	}

	@Test
	void misplacedImports() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeUnsortedMisplacedImports.test", "java/importsorter/JavaCodeSortedMisplacedImports.test");
	}

	@Test
	void empty() throws Throwable {
		FormatterStep step = ImportOrderStep.forJava().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/JavaCodeEmptyFile.test", "java/importsorter/JavaCodeEmptyFile.test");
	}

	@Test
	void groovyImports() throws Throwable {
		FormatterStep step = ImportOrderStep.forGroovy().createFrom(createTestFile("java/importsorter/import.properties"));
		assertOnResources(step, "java/importsorter/GroovyCodeUnsortedMisplacedImports.test", "java/importsorter/GroovyCodeSortedMisplacedImports.test");
	}

	@Test
	void doesntThrowIfImportOrderIsntSerializable() {
		ImportOrderStep.forJava().createFrom("java", "javax", "org", "\\#com");
	}

	@Test
	void equality() throws Exception {
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
