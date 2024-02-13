/*
 * Copyright 2016-2024 DiffPlug
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

import static org.assertj.core.api.Assertions.assertThat;

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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.util.CheckReturnValue;
import org.junit.jupiter.api.io.TempDir;

import com.diffplug.common.base.Errors;
import com.diffplug.common.io.Resources;

public class ResourceHarness {
	/**
	 * On OS X, the temp folder is a symlink,
	 * and some of gradle's stuff breaks symlinks.
	 * By only accessing it through the {@link #rootFolder()}
	 * and {@link #newFile(String)} apis, we can guarantee there
	 * will be no symlink problems.
	 */
	@TempDir
	File folderDontUseDirectly;

	/** Returns the root folder (canonicalized to fix OS X issue) */
	protected File rootFolder() {
		return Errors.rethrow().get(() -> folderDontUseDirectly.getCanonicalFile());
	}

	/** Returns a new child of the root folder. */
	protected File newFile(String subpath) {
		return new File(rootFolder(), subpath);
	}

	/** Creates and returns a new child-folder of the root folder. */
	protected File newFolder(String subpath) throws IOException {
		File targetDir = newFile(subpath);
		if (!targetDir.mkdir()) {
			throw new IOException("Failed to create " + targetDir);
		}
		return targetDir;
	}

	protected String read(String path) throws IOException {
		return read(newFile(path).toPath(), StandardCharsets.UTF_8);
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
		setFile(path).toContent(after);
	}

	/** Returns the contents of the given file from the src/test/resources directory. */
	protected static String getTestResource(String filename) {
		Optional<URL> resourceUrl = getTestResourceUrl(filename);
		if (resourceUrl.isPresent()) {
			return ThrowingEx.get(() -> LineEnding.toUnix(Resources.toString(resourceUrl.get(), StandardCharsets.UTF_8)));
		}
		throw new IllegalArgumentException("No such resource " + filename);
	}

	protected static boolean existsTestResource(String filename) {
		return getTestResourceUrl(filename).isPresent();
	}

	private static Optional<URL> getTestResourceUrl(String filename) {
		URL url = ResourceHarness.class.getResource("/" + filename);
		return Optional.ofNullable(url);
	}

	/** Returns Files (in a temporary folder) which has the contents of the given file from the src/test/resources directory. */
	protected List<File> createTestFiles(String... filenames) {
		List<File> files = new ArrayList<>(filenames.length);
		for (String filename : filenames) {
			files.add(createTestFile(filename));
		}
		return files;
	}

	/** Returns a File (in a temporary folder) which has the contents of the given file from the src/test/resources directory. */
	protected File createTestFile(String filename) {
		return createTestFile(filename, UnaryOperator.identity());
	}

	/**
	 * Returns a File (in a temporary folder) which has the contents, possibly processed, of the given file from the
	 * src/test/resources directory.
	 */
	protected File createTestFile(String filename, UnaryOperator<String> fileContentsProcessor) {
		int lastSlash = filename.lastIndexOf('/');
		String name = lastSlash >= 0 ? filename.substring(lastSlash) : filename;
		File file = newFile(name);
		file.getParentFile().mkdirs();
		ThrowingEx.run(() -> Files.write(file.toPath(), fileContentsProcessor.apply(getTestResource(filename)).getBytes(StandardCharsets.UTF_8)));
		return file;
	}

	@CheckReturnValue
	protected ReadAsserter assertFile(String path) {
		return new ReadAsserter(newFile(path));
	}

	@CheckReturnValue
	protected ReadAsserter assertFile(File file) {
		return new ReadAsserter(file);
	}

	public static class ReadAsserter {
		private final File file;

		private ReadAsserter(File file) {
			this.file = file;
		}

		public void hasContent(String expected) {
			hasContent(expected, StandardCharsets.UTF_8);
		}

		public void hasDifferentContent(String expected) {
			hasDifferentContent(expected, StandardCharsets.UTF_8);
		}

		public void hasContent(String expected, Charset charset) {
			assertThat(file).usingCharset(charset).hasContent(expected);
		}

		public void hasDifferentContent(String expected, Charset charset) {
			assertThat(file).usingCharset(charset).isNotEqualTo(expected);
		}

		public void hasLines(String... lines) {
			hasContent(String.join("\n", Arrays.asList(lines)));
		}

		public void sameAsResource(String resource) throws IOException {
			hasContent(getTestResource(resource));
		}

		public void notSameAsResource(String resource) throws IOException {
			hasDifferentContent(getTestResource(resource));
		}

		public void matches(Consumer<AbstractCharSequenceAssert<?, String>> conditions) throws IOException {
			String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			conditions.accept(assertThat(content));
		}
	}

	protected WriteAsserter setFile(String path) {
		return new WriteAsserter(newFile(path));
	}

	public static class WriteAsserter {
		private File file;

		private WriteAsserter(File file) {
			file.getParentFile().mkdirs();
			this.file = file;
		}

		public File toLines(String... lines) {
			return toContent(String.join("\n", Arrays.asList(lines)));
		}

		public File toContent(String content) {
			return toContent(content, StandardCharsets.UTF_8);
		}

		public File toContent(String content, Charset charset) {
			ThrowingEx.run(() -> {
				Files.write(file.toPath(), content.getBytes(charset));
			});
			return file;
		}

		public File toResource(String path) {
			ThrowingEx.run(() -> {
				Files.write(file.toPath(), getTestResource(path).getBytes(StandardCharsets.UTF_8));
			});
			return file;
		}

		public File deleted() throws IOException {
			Files.delete(file.toPath());
			return file;
		}
	}
}
