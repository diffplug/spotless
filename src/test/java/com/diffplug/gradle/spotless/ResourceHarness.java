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
package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Throwing;
import com.diffplug.common.io.Resources;

public class ResourceHarness {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/** Returns the contents of the given file from the src/test/resources directory. */
	protected String getTestResource(String filename) throws IOException {
		return getTestResource(filename, LineEnding.UNIX);
	}

	protected String getTestResource(String filename, LineEnding ending) throws IOException {
		String raw = Resources.toString(ResourceHarness.class.getResource("/" + filename), StandardCharsets.UTF_8);
		return LineEnding.toUnix(raw).replace("\n", ending.str());
	}

	/** Returns a File (in a temporary folder) which has the contents of the given file from the src/test/resources directory. */
	protected File createTestFile(String filename) throws IOException {
		int lastSlash = filename.lastIndexOf('/');
		String name = lastSlash >= 0 ? filename.substring(lastSlash) : filename;
		File file = folder.newFile(name);
		Files.write(file.toPath(), getTestResource(filename).getBytes(StandardCharsets.UTF_8));
		return file;
	}

	/** Returns a File (in a temporary folder) which has the given contents. */
	protected File createTestFile(String filename, String content) throws IOException {
		File file = folder.newFile(filename);
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return file;
	}

	/** Asserts that the given resource from the src/test/resources directory has the same content as the given file. */
	protected void assertFileContent(String expectedContent, File actual) throws IOException {
		// This line thing is necessary for the tests to pass when Windows git screws up the line-endings
		String actualContent = new String(Files.readAllBytes(actual.toPath()), StandardCharsets.UTF_8);
		Assert.assertEquals(expectedContent, actualContent);
	}

	/** Reads the given resource from "before", applies the step, and makes sure the result is "after". */
	protected void assertStep(Throwing.Function<String, String> step, String unformattedPath, String expectedPath) throws Throwable {
		String unformatted = LineEnding.toUnix(getTestResource(unformattedPath)); // unix-ified input
		String formatted = step.apply(unformatted);
		// no windows newlines
		Assert.assertEquals(-1, formatted.indexOf('\r'));

		// unix-ify the test resource output in case git screwed it up
		String expected = LineEnding.toUnix(getTestResource(expectedPath)); // unix-ified output
		Assert.assertEquals(expected, formatted);
	}

	/** Creates a FormatTask based on the given consumer. */
	public static FormatTask createTask(Consumer<FormatExtension> test) throws Exception {
		Project project = ProjectBuilder.builder().build();
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);

		AtomicReference<FormatExtension> ref = new AtomicReference<>();
		plugin.getExtension().format("underTest", ext -> {
			ref.set(ext);
			test.accept(ext);
		});

		boolean check = false;
		return plugin.createTask("underTest", ref.get(), check);
	}

	/** Tests that the formatExtension causes the given change. */
	protected void assertTask(Consumer<FormatExtension> test, String before, String afterExpected) throws Exception {
		// create the task
		FormatTask task = createTask(test);
		// force unix line endings, since we're passing in raw strings
		task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		// create the test file
		File testFile = folder.newFile();
		Files.write(testFile.toPath(), before.getBytes(StandardCharsets.UTF_8));
		// set the task to use this test file
		task.target = Collections.singleton(testFile);
		// run the task
		task.format();
		// check what the task did
		String afterActual = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
		Assert.assertEquals(afterExpected, afterActual);
	}
}
