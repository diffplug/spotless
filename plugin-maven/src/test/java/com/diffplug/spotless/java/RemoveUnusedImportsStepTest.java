/*
 * Copyright 2016-2025 DiffPlug
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

import static com.diffplug.spotless.java.RemoveUnusedImportsStep.DEFAULT_FORMATTER;
import static com.diffplug.spotless.java.RemoveUnusedImportsStep.NAME;
import static com.diffplug.spotless.java.RemoveUnusedImportsStep.defaultFormatter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.maven.MavenIntegrationHarness;

class RemoveUnusedImportsStepTest extends MavenIntegrationHarness {

	@Test
	void testRemoveUnusedImports() throws Exception {
		writePomWithJavaSteps("<removeUnusedImports/>");

		String path = "src/main/java/test.java";
		setFile(path).toResource("java/removeunusedimports/JavaCodeWithPackageUnformatted.test");
		mavenRunner().withArguments("spotless:apply").runNoError();
		assertFile(path).sameAsResource("java/removeunusedimports/JavaCodeWithPackageFormatted.test");
	}

	@Test
	void testDefaults() {
		assertEquals("palantir-java-format", defaultFormatter());
		assertEquals("palantir-java-format", DEFAULT_FORMATTER);
		assertEquals("removeUnusedImports", NAME);
	}

	@Test
	void testCreateWithDefaultFormatter() {
		assertEquals(NAME, RemoveUnusedImportsStep.create((groupArtifact, version) -> Collections.emptySet()).getName());
	}

	@Test
	void testCreateWithPalantirFormatter() {
		assertEquals(NAME, RemoveUnusedImportsStep.create("palantir-java-format", (groupArtifact, version) -> Collections.emptySet()).getName());
	}

	@Test
	void testCreateWithCleanthatFormatter() {
		assertEquals(NAME, RemoveUnusedImportsStep.create("cleanthat-javaparser-unnecessaryimport", (groupArtifact, version) -> Collections.emptySet()).getName());
	}

	@Test
	void testCreateWithInvalidFormatter() {
		assertThrows(IllegalArgumentException.class, () -> RemoveUnusedImportsStep.create("invalid-formatter", (groupArtifact, version) -> Collections.emptySet()));
	}

	@Test
	void testCreateWithNullProvisioner() {
		assertThrows(NullPointerException.class, () -> RemoveUnusedImportsStep.create("palantir-java-format", null));
	}
}
