/*
 * Copyright 2025 DiffPlug
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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarnessWithFile;
import com.diffplug.spotless.TestProvisioner;

public class ExpandWildcardImportsStepTest extends ResourceHarness {

	@Test
	void expandWildCardImports() throws Exception {
		newFile("src/foo/bar/baz/").mkdirs();
		Files.write(newFile("src/foo/bar/AnotherClassInSamePackage.java").toPath(), getTestResource("java/expandwildcardimports/AnotherClassInSamePackage.test").getBytes(StandardCharsets.UTF_8));
		Files.write(newFile("src/foo/bar/baz/AnotherImportedClass.java").toPath(), getTestResource("java/expandwildcardimports/AnotherImportedClass.test").getBytes(StandardCharsets.UTF_8));
		File dummyJar = new File(ResourceHarness.class.getResource("/java/expandwildcardimports/example-lib.jar").toURI());
		FormatterStep step = ExpandWildcardImportsStep.create(Set.of(newFile("src"), dummyJar), TestProvisioner.mavenCentral());
		StepHarnessWithFile.forStep(this, step).testResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test", "java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");
	}

}
