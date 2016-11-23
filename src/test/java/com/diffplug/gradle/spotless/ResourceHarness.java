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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Throwing;
import com.diffplug.common.collect.Iterables;
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
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), getTestResource(filename).getBytes(StandardCharsets.UTF_8));
		return file;
	}

	/** Returns a File (in a temporary folder) which has the given contents. */
	protected File createTestFile(String filename, String content) throws IOException {
		File file = new File(folder.getRoot(), filename);
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
	protected void assertStep(Throwing.Function<String, String> step, String unformattedPath, String expectedPath) throws Throwable {
		String unformatted = LineEnding.toUnix(getTestResource(unformattedPath)); // unix-ified input
		String formatted = step.apply(unformatted);
		// no windows newlines
		Assert.assertEquals(-1, formatted.indexOf('\r'));

		// unix-ify the test resource output in case git screwed it up
		String expected = LineEnding.toUnix(getTestResource(expectedPath)); // unix-ified output
		Assert.assertEquals(expected, formatted);
	}

	/** Creates an ApplyFormatTask based on the given consumer. */
	private ApplyFormatTask createApplyTask(Consumer<FormatExtension> test) throws Exception {
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);

		AtomicReference<FormatExtension> ref = new AtomicReference<>();
		plugin.getExtension().format("underTest", ext -> {
			ext.setLineEndings(LineEnding.UNIX);
			ref.set(ext);
			test.accept(ext);
		});

		return plugin.createApplyTask("underTest", ref.get());
	}

	/** Tests that the formatExtension causes the given change. */
	protected void assertTask(Consumer<FormatExtension> test, String before, String afterExpected) throws Exception {
		// create the task
		ApplyFormatTask task = createApplyTask(test);
		// force unix line endings, since we're passing in raw strings
		task.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
		// create the test file
		File testFile = folder.newFile();
		Files.write(testFile.toPath(), before.getBytes(StandardCharsets.UTF_8));
		// set the task to use this test file
		task.setTarget(Collections.singleton(testFile));
		// run the task
		task.apply();
		// check what the task did
		String afterActual = new String(Files.readAllBytes(testFile.toPath()), StandardCharsets.UTF_8);
		Assert.assertEquals(afterExpected, afterActual);
	}

	protected String getTaskErrorMessage(BaseFormatTask task) {
		try {
			task.execute();
			throw new AssertionError("Expected a TaskExecutionException");
		} catch (TaskExecutionException e) {
			GradleException cause = (GradleException) e.getCause();
			return cause.getMessage();
		}
	}

	/** Creates a collection of CheckFormatTask based on the given extension configuration. */
	protected List<CheckFormatTask> createCheckTasks(Consumer<SpotlessExtension> test) throws Exception {
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		project.getRepositories().mavenCentral(); // ensures that plugins which resolve from mavenCentral will work
		project.getPlugins().apply(JavaBasePlugin.class); // ensures that the java extension will work
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);
		test.accept(plugin.getExtension());
		plugin.createTasks();
		return new ArrayList<>(project.getTasks().withType(CheckFormatTask.class).getAsMap().values());
	}

	/** Creates a single of CheckFormatTask based on the given extension configuration. */
	protected CheckFormatTask createCheckTask(Consumer<SpotlessExtension> test) throws Exception {
		return Iterables.getOnlyElement(createCheckTasks(test));
	}
}
