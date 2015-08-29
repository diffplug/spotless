/*
 * Copyright 2015 DiffPlug
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

import groovy.lang.Closure;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class ResourceTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/** Creates a FormatTask based on the given consumer. */
	public static FormatTask createTask(final SimpleConsumer<FormatExtension> test) throws Exception {
		Project project = ProjectBuilder.builder().build();
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);

		final AtomicReference<FormatExtension> ref = new AtomicReference<>();
		plugin.getExtension().format("underTest", new Closure<FormatExtension>(plugin) {
			@Override
			public FormatExtension call() {
				FormatExtension extension = (FormatExtension) getDelegate();
				ref.set(extension);
				test.accept(extension);
				return extension;
			}
		});

		boolean check = false;
		return plugin.createTask("underTest", ref.get(), check);
	}

	/** Returns the contents of the given file from the src/test/resources directory. */
	protected String getTestResource(String filename) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream inputStream = getClass().getResourceAsStream("/" + filename);
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
			baos.write(buffer, 0, length);
		}
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
	}

	/** Returns a File (in a temporary folder) which has the contents of the given file from the src/test/resources directory. */
	protected File createTestFile(String filename) throws IOException {
		File file = folder.newFile(filename);
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
	protected void assertStep(FormattingOperation step, String unformattedPath, String expectedPath) throws Throwable {
		String unformatted = getTestResource(unformattedPath).replace("\r", ""); // unix-ified input
		String formatted = step.apply(unformatted);
		// no windows newlines
		Assert.assertEquals(-1, formatted.indexOf('\r'));

		// unix-ify the test resource output in case git screwed it up
		String expected = getTestResource(expectedPath).replace("\r", ""); // unix-ified output
		Assert.assertEquals(expected, formatted);
	}

	/** Tests that the formatExtension causes the given change. */
	protected void assertTask(final SimpleConsumer<FormatExtension> test, final String before, final String afterExpected) throws Exception {
		// create the task
		FormatTask task = createTask(test);
		// force unix line endings, since we're passing in raw strings
		task.lineEndings = LineEnding.UNIX;
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
