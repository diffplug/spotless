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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

public abstract class ResourceTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/** Returns the contents of the given file from the src/test/resources directory. */
	protected String getTestResource(String filename) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream inputStream = getClass().getResourceAsStream("/" + filename);
		byte[] buffer = new byte[1024];
		int length = 0;
		while ((length = inputStream.read(buffer)) != -1) {
			baos.write(buffer, 0, length);
		}
		return new String(baos.toByteArray(), StandardCharsets.UTF_8);
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
		String unformatted = getTestResource(unformattedPath);
		String expected = getTestResource(expectedPath);

		String formatted = step.apply(unformatted);

		Assert.assertEquals(expected, formatted);
	}

}
