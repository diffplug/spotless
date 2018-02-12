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
package com.diffplug.spotless;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.assertj.core.api.AbstractFileAssert;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.ComparisonFailure;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.diffplug.common.io.Resources;

public class ResourceHarness {
	/**
	 * On OS X, the temp folder is a symlink,
	 * and some of gradle's stuff breaks symlinks.
	 * By only accessing it through the {@link #rootFolder()}
	 * and {@link #newFile()} apis, we can guarantee there
	 * will be no symlink problems.
	 */
	@Rule
	public TemporaryFolder folderDontUseDirectly = new TemporaryFolder();

	/** Log nontruncated diff in case of a comparison failure to ease test development.*/
	@Rule
	public TestWatcher logComparisonFailureDiff = new TestWatcher() {
		private static final String COMPARISON_SEPARATOR = "------------------------------------";

		@Override
		protected void failed(Throwable e, Description description) {
			if (e instanceof ComparisonFailure) {
				ComparisonFailure failure = (ComparisonFailure) e;
				String msg = "";
				msg += String.format("Output:  %n%1$s%n%2$s%n%1$s%n", COMPARISON_SEPARATOR, failure.getActual());
				msg += String.format("Expected:%n%1$s%n%2$s%n%1$s%n", COMPARISON_SEPARATOR, failure.getExpected());
				logFailure(msg, description);
			}

		}

		private void logFailure(String message, Description description) {
			Logger log = Logger.getLogger(description.getClassName());
			log.warning(String.format("Step '%s' failed.%n%s", description.getDisplayName(), message));
		}

	};

	/** Returns the root folder (canonicalized to fix OS X issue) */
	protected File rootFolder() throws IOException {
		return folderDontUseDirectly.getRoot().getCanonicalFile();
	}

	/** Returns a new child of the root folder. */
	protected File newFile(String subpath) throws IOException {
		return new File(rootFolder(), subpath);
	}

	/** Returns a random child of the root folder. */
	protected File newFile() throws IOException {
		return folderDontUseDirectly.newFile().getCanonicalFile();
	}

	/** Writes the given content to the given path. */
	protected File write(String path, String... lines) throws IOException {
		return write(path, LineEnding.UNIX, lines);
	}

	protected File write(String path, LineEnding ending, String... lines) throws IOException {
		return write(path, ending, StandardCharsets.UTF_8, lines);
	}

	protected File write(String path, LineEnding ending, Charset encoding, String... lines) throws IOException {
		String content;
		if (lines.length == 1) {
			content = lines[0];
		} else {
			content = Arrays.stream(lines).collect(Collectors.joining(ending.str())) + ending.str();
		}
		Path target = newFile(path).toPath();
		Files.createDirectories(target.getParent());
		Files.write(target, content.getBytes(encoding));
		return target.toFile();
	}

	protected AbstractFileAssert<?> assertFile(String path) throws IOException {
		return Assertions.assertThat(newFile(path)).usingCharset(StandardCharsets.UTF_8);
	}

	protected String read(String path) throws IOException {
		return read(newFile(path).toPath());
	}

	protected String read(Path path) throws IOException {
		return read(path, StandardCharsets.UTF_8);
	}

	protected String read(String path, Charset encoding) throws IOException {
		return read(newFile(path).toPath(), encoding);
	}

	protected String read(Path path, Charset encoding) throws IOException {
		return new String(Files.readAllBytes(path), encoding);
	}

	protected void replace(String path, String toReplace, String replaceWith) throws IOException {
		String before = read(path);
		String after = before.replace(toReplace, replaceWith);
		if (before.equals(after)) {
			throw new IllegalArgumentException("Replace was ineffective! '" + toReplace + "' was not found in " + path);
		}
		write(path, after);
	}

	/** Returns the contents of the given file from the src/test/resources directory. */
	protected static String getTestResource(String filename) throws IOException {
		URL url = ResourceHarness.class.getResource("/" + filename);
		if (url == null) {
			throw new IllegalArgumentException("No such resource " + filename);
		}
		return Resources.toString(url, StandardCharsets.UTF_8);
	}

	/** Returns Files (in a temporary folder) which has the contents of the given file from the src/test/resources directory. */
	protected List<File> createTestFiles(String... filenames) throws IOException {
		List<File> files = new ArrayList<>(filenames.length);
		for (String filename : filenames) {
			files.add(createTestFile(filename));
		}
		return files;
	}

	/** Returns a File (in a temporary folder) which has the contents of the given file from the src/test/resources directory. */
	protected File createTestFile(String filename) throws IOException {
		int lastSlash = filename.lastIndexOf('/');
		String name = lastSlash >= 0 ? filename.substring(lastSlash) : filename;
		File file = newFile(name);
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), getTestResource(filename).getBytes(StandardCharsets.UTF_8));
		return file;
	}

	/** Returns a File (in a temporary folder) which has the given contents. */
	protected File createTestFile(String filename, String content) throws IOException {
		File file = newFile(filename);
		file.getParentFile().mkdirs();
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
	protected void assertOnResources(FormatterStep step, String unformattedPath, String expectedPath) throws Throwable {
		assertOnResources(rawUnix -> step.format(rawUnix, new File("")), unformattedPath, expectedPath);
	}

	/** Reads the given resource from "before", applies the step, and makes sure the result is "after". */
	protected void assertOnResources(FormatterFunc step, String unformattedPath, String expectedPath) throws Throwable {
		String unformatted = LineEnding.toUnix(getTestResource(unformattedPath)); // unix-ified input
		String formatted = step.apply(unformatted);
		// no windows newlines
		Assert.assertEquals(-1, formatted.indexOf('\r'));

		// unix-ify the test resource output in case git screwed it up
		String expected = LineEnding.toUnix(getTestResource(expectedPath)); // unix-ified output
		Assert.assertEquals(expected, formatted);
	}
}
