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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.diffplug.common.collect.Iterables;
import com.diffplug.common.io.Resources;

public class ResourceHarness {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/** Returns the contents of the given file from the src/test/resources directory. */
	protected String getTestResource(String filename) {
		return getTestResource(filename, LineEnding.UNIX);
	}

	protected String getTestResource(String filename, LineEnding ending) {
		String raw = Errors.rethrow().get(() -> Resources.toString(ResourceHarness.class.getResource("/" + filename), StandardCharsets.UTF_8));
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
	protected void assertOnResources(Throwing.Function<String, String> step, String unformattedPath, String expectedPath) throws Throwable {
		String unformatted = LineEnding.toUnix(getTestResource(unformattedPath)); // unix-ified input
		String formatted = step.apply(unformatted);
		// no windows newlines
		Assert.assertEquals(-1, formatted.indexOf('\r'));

		// unix-ify the test resource output in case git screwed it up
		String expected = LineEnding.toUnix(getTestResource(expectedPath)); // unix-ified output
		Assert.assertEquals(expected, formatted);
	}

	/** Runs a test case on the task created by this extension. */
	protected void assertTask(Consumer<SpotlessExtension> test, String before, String after) throws Exception {
		assertTask(test, api -> api.add(before, after));
	}

	/** An api for adding test cases. */
	public interface TestCaseAPI {
		void add(String before, String after);

		default void add(String noChange) {
			add(noChange, noChange);
		}
	}

	/** Runs many test cases on the task created by this extension. */
	protected void assertTask(Consumer<SpotlessExtension> test, Consumer<TestCaseAPI> testCases) throws Exception {
		List<String> befores = new ArrayList<>();
		List<String> afters = new ArrayList<>();
		testCases.accept((before, after) -> {
			befores.add(before);
			afters.add(after);
		});

		// create the task
		ApplyFormatTask task = createApplyTask(test);
		// create the test file(s)
		List<File> files = new ArrayList<>(befores.size());
		for (String before : befores) {
			File testFile = folder.newFile();
			Files.write(testFile.toPath(), before.getBytes(StandardCharsets.UTF_8));
			files.add(testFile);
		}
		// set the task to use this test file
		task.setTarget(files);
		// run the task
		task.apply();
		// check what the task did
		for (int i = 0; i < befores.size(); ++i) {
			String afterExpected = afters.get(i);
			String afterActual = new String(Files.readAllBytes(files.get(i).toPath()), StandardCharsets.UTF_8);
			Assert.assertEquals(afterExpected, afterActual);
		}
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
	protected <T extends Task> List<T> createTasks(Consumer<SpotlessExtension> test, Class<T> clazz) throws Exception {
		// write out the .gitattributes file
		Files.write(
				folder.getRoot().toPath().resolve(".gitattributes"),
				"* text eol=lf".getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		// create the file and tasks
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		project.getRepositories().mavenCentral(); // ensures that plugins which resolve from mavenCentral will work
		project.getPlugins().apply(JavaBasePlugin.class); // ensures that the java extension will work
		SpotlessPlugin plugin = project.getPlugins().apply(SpotlessPlugin.class);
		test.accept(plugin.getExtension());
		plugin.createTasks();
		return new ArrayList<>(project.getTasks().withType(clazz).getAsMap().values());
	}

	/** Creates a single of CheckFormatTask based on the given extension configuration. */
	protected CheckFormatTask createCheckTask(Consumer<SpotlessExtension> test) throws Exception {
		return Iterables.getOnlyElement(createTasks(test, CheckFormatTask.class));
	}

	/** Creates a collection of CheckFormatTask based on the given extension configuration. */
	protected ApplyFormatTask createApplyTask(Consumer<SpotlessExtension> test) throws Exception {
		return Iterables.getOnlyElement(createTasks(test, ApplyFormatTask.class));
	}
}
