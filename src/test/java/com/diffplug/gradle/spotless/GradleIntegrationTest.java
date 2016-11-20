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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.tree.TreeDef;
import com.diffplug.common.tree.TreeStream;

public class GradleIntegrationTest extends ResourceHarness {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	protected File write(String path, String... lines) throws IOException {
		return write(path, LineEnding.UNIX, lines);
	}

	protected File write(String path, LineEnding ending, String... lines) throws IOException {
		return write(path, ending, StandardCharsets.UTF_8, lines);
	}

	protected File write(String path, LineEnding ending, Charset encoding, String... lines) throws IOException {
		String content = Arrays.stream(lines).collect(Collectors.joining(ending.str())) + ending.str();
		Path target = folder.getRoot().toPath().resolve(path);
		Files.createDirectories(target.getParent());
		Files.write(target, content.getBytes(encoding));
		return target.toFile();
	}

	protected String read(String path) throws IOException {
		return read(path, LineEnding.UNIX);
	}

	protected String read(String path, LineEnding ending) throws IOException {
		return read(path, ending, StandardCharsets.UTF_8);
	}

	protected String read(String path, LineEnding ending, Charset encoding) throws IOException {
		Path target = folder.getRoot().toPath().resolve(path);
		String content = new String(Files.readAllBytes(target), encoding);
		String allUnixNewline = LineEnding.toUnix(content);
		return allUnixNewline.replace("\n", ending.str());
	}

	protected File file(String path) {
		return folder.getRoot().toPath().resolve(path).toFile();
	}

	protected GradleRunner gradleRunner() {
		return GradleRunner.create().withProjectDir(folder.getRoot()).withPluginClasspath();
	}

	/** Dumps the complete file contents of the folder to the console. */
	protected String getContents() throws IOException {
		return getContents(subPath -> !subPath.startsWith(".gradle"));
	}

	protected String getContents(Predicate<String> subpathsToInclude) throws IOException {
		TreeDef<File> treeDef = TreeDef.forFile(Errors.rethrow());
		List<File> files = TreeStream.depthFirst(treeDef, folder.getRoot())
				.filter(File::isFile)
				.collect(Collectors.toList());

		ListIterator<File> iterator = files.listIterator(files.size());
		int rootLength = folder.getRoot().getAbsolutePath().length() + 1;
		return StringPrinter.buildString(printer -> Errors.rethrow().run(() -> {
			while (iterator.hasPrevious()) {
				File file = iterator.previous();
				String subPath = file.getAbsolutePath().substring(rootLength);
				if (subpathsToInclude.test(subPath)) {
					printer.println("### " + subPath + " ###");
					printer.println(read(subPath));
				}
			}
		}));
	}

	protected void checkRunsThenUpToDate() throws IOException {
		checkIsUpToDate(false);
		checkIsUpToDate(true);
	}

	protected void checkIsUpToDate(boolean upToDate) throws IOException {
		// first run of spotlessCheck
		BuildResult buildResult = gradleRunner().withArguments("spotlessCheck").build();

		TaskOutcome expected = upToDate ? TaskOutcome.UP_TO_DATE : TaskOutcome.SUCCESS;
		TaskOutcome notExpected = upToDate ? TaskOutcome.SUCCESS : TaskOutcome.UP_TO_DATE;

		Assertions.assertThat(buildResult.tasks(expected)).isNotEmpty();
		Assertions.assertThat(buildResult.tasks(notExpected)).isEmpty();
		Assertions.assertThat(buildResult.getTasks()).hasSameSizeAs(buildResult.tasks(expected));
	}
}
