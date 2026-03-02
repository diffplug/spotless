/*
 * Copyright 2025-2026 DiffPlug
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

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;
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

	@Test
	void expandWildcardImports_concurrentAccess() throws Exception {
		// Test concurrent access to the formatter step
		newFile("src/foo/bar/baz/").mkdirs();
		Files.write(newFile("src/foo/bar/AnotherClassInSamePackage.java").toPath(), getTestResource("java/expandwildcardimports/AnotherClassInSamePackage.test").getBytes(StandardCharsets.UTF_8));
		Files.write(newFile("src/foo/bar/baz/AnotherImportedClass.java").toPath(), getTestResource("java/expandwildcardimports/AnotherImportedClass.test").getBytes(StandardCharsets.UTF_8));
		File dummyJar = new File(ResourceHarness.class.getResource("/java/expandwildcardimports/example-lib.jar").toURI());

		// Create the step once
		FormatterStep step = ExpandWildcardImportsStep.create(
				Collections.synchronizedSet(new HashSet<>(Set.of(newFile("src"), dummyJar))),
				TestProvisioner.mavenCentral());

		String testInput = getTestResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");
		String expectedOutput = getTestResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");

		// Test with multiple threads
		int threadCount = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		AtomicInteger successCount = new AtomicInteger(0);

		for (int i = 0; i < threadCount; i++) {
			executor.submit(() -> {
				try {
					StepHarness harness = StepHarness.forStep(step);
					harness.test(testInput, expectedOutput);
					successCount.incrementAndGet();
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			});
		}

		executor.shutdown();
		executor.awaitTermination(30, TimeUnit.SECONDS);

		// All threads should succeed
		assertEquals(threadCount, successCount.get(), "All concurrent accesses should succeed");
	}

	@Test
	void expandWildcardImports_withMutableClasspath() throws Exception {
		// Test with a classpath that could be modified concurrently
		newFile("src/foo/bar/baz/").mkdirs();
		Files.write(newFile("src/foo/bar/AnotherClassInSamePackage.java").toPath(), getTestResource("java/expandwildcardimports/AnotherClassInSamePackage.test").getBytes(StandardCharsets.UTF_8));
		Files.write(newFile("src/foo/bar/baz/AnotherImportedClass.java").toPath(), getTestResource("java/expandwildcardimports/AnotherImportedClass.test").getBytes(StandardCharsets.UTF_8));
		File dummyJar = new File(ResourceHarness.class.getResource("/java/expandwildcardimports/example-lib.jar").toURI());

		// Use thread-safe collections for classpath
		Set<File> classpath = ConcurrentHashMap.newKeySet();
		classpath.add(newFile("src"));
		classpath.add(dummyJar);

		FormatterStep step = ExpandWildcardImportsStep.create(classpath, TestProvisioner.mavenCentral());

		String testInput = getTestResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");
		String expectedOutput = getTestResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");

		StepHarness.forStep(step).test(testInput, expectedOutput);

		// Test that we can modify the original classpath without affecting the step
		// (the step should have taken a defensive copy)
		classpath.clear();
		classpath.add(newFile("different/path"));

		// The step should still work with its original classpath
		StepHarness.forStep(step).test(testInput, expectedOutput);
	}

	@Test
	void expandWildcardImports_emptyClasspath() throws Exception {
		// Test with empty classpath
		FormatterStep step = ExpandWildcardImportsStep.create(Collections.emptySet(), TestProvisioner.mavenCentral());

		// Even with empty classpath, Java standard library imports should still be expanded
		String simpleCode = """
				package test;

				import java.util.*;

				public class Test {
					private List<String> items;
				}
				""";

		String expectedOutput = """
				package test;

				import java.util.List;

				public class Test {
					private List<String> items;
				}
				""";

		// The step should still expand java.util.* to java.util.List
		StepHarness.forStep(step).test(simpleCode, expectedOutput);
	}

	@Test
	void expandWildcardImports_nullSafety() throws Exception {
		// Test null safety
		try {
			ExpandWildcardImportsStep.create(null, TestProvisioner.mavenCentral());
			fail("Should throw NullPointerException for null classpath");
		} catch (NullPointerException e) {
			// Expected
		}

		try {
			ExpandWildcardImportsStep.create(Set.of(newFile("src")), null);
			fail("Should throw NullPointerException for null provisioner");
		} catch (NullPointerException e) {
			// Expected
		}
	}

	@Test
	void expandWildcardImports_withLargeClasspath() throws Exception {
		// Test with a large classpath to ensure no performance issues
		newFile("src/foo/bar/baz/").mkdirs();
		Files.write(newFile("src/foo/bar/AnotherClassInSamePackage.java").toPath(), getTestResource("java/expandwildcardimports/AnotherClassInSamePackage.test").getBytes(StandardCharsets.UTF_8));
		Files.write(newFile("src/foo/bar/baz/AnotherImportedClass.java").toPath(), getTestResource("java/expandwildcardimports/AnotherImportedClass.test").getBytes(StandardCharsets.UTF_8));
		File dummyJar = new File(ResourceHarness.class.getResource("/java/expandwildcardimports/example-lib.jar").toURI());

		// Create a large classpath
		Set<File> largeClasspath = ConcurrentHashMap.newKeySet();
		largeClasspath.add(newFile("src"));
		largeClasspath.add(dummyJar);

		// Add many dummy directories
		for (int i = 0; i < 100; i++) {
			File dummyDir = newFile("dummy/dir/" + i + "/");
			dummyDir.mkdirs();
			largeClasspath.add(dummyDir);
		}

		FormatterStep step = ExpandWildcardImportsStep.create(largeClasspath, TestProvisioner.mavenCentral());

		String testInput = getTestResource("java/expandwildcardimports/JavaClassWithWildcardsUnformatted.test");
		String expectedOutput = getTestResource("java/expandwildcardimports/JavaClassWithWildcardsFormatted.test");

		// Should handle large classpaths without issues
		StepHarness.forStep(step).test(testInput, expectedOutput);
	}
}
