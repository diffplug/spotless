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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;

public class CheckFailedDiffTest extends ResourceHarness {
	private FormatTask create(File... files) {
		return create(Arrays.asList(files));
	}

	private FormatTask create(List<File> files) {
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		FormatTask task = project.getTasks().create("underTest", FormatTask.class);
		task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		task.check = true;
		task.target = files;
		return task;
	}

	private void assertTaskFailure(FormatTask task, String... expectedLines) {
		try {
			task.execute();
		} catch (TaskExecutionException e) {
			GradleException cause = (GradleException) e.getCause();
			String msg = cause.getMessage();
			String firstLine = "The following files had format violations:\n";
			String lastLine = "\nFormat violations were found. Run 'gradlew spotlessApply' to fix them.";
			Assertions.assertThat(msg).startsWith(firstLine).endsWith(lastLine);

			String middle = msg.substring(firstLine.length(), msg.length() - lastLine.length());
			String expectedMessage = StringPrinter.buildStringFromLines(expectedLines);
			Assertions.assertThat(middle).isEqualTo(expectedMessage.substring(0, expectedMessage.length() - 1));
		}
	}

	@Test
	public void lineEndingProblem() throws IOException {
		FormatTask task = create(createTestFile("testFile", "A\r\nB\r\nC\r\n"));
		assertTaskFailure(task,
				"    testFile",
				"    @@ -1,3 +1,3 @@",
				"    -A\\r\\n",
				"    -B\\r\\n",
				"    -C\\r\\n",
				"    +A\\n",
				"    +B\\n",
				"    +C\\n");
	}

	@Test
	public void whitespaceProblem() throws IOException {
		FormatTask task = create(createTestFile("testFile", "A \nB\t\nC  \n"));
		task.steps.add(FormatterStep.create("trimTrailing", input -> {
			Pattern pattern = Pattern.compile("[ \t]+$", Pattern.UNIX_LINES | Pattern.MULTILINE);
			return pattern.matcher(input).replaceAll("");
		}));
		assertTaskFailure(task,
				"    testFile",
				"    @@ -1,3 +1,3 @@",
				"    -A·",
				"    -B\\t",
				"    -C··",
				"    +A",
				"    +B",
				"    +C");
	}

	@Test
	public void multipleFiles() throws IOException {
		FormatTask task = create(
				createTestFile("A", "1\r\n2\r\n"),
				createTestFile("B", "3\n4\r\n"));
		assertTaskFailure(task,
				"    A",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    B",
				"    @@ -1,2 +1,2 @@",
				"     3\\n",
				"    -4\\r\\n",
				"    +4\\n");
	}

	@Test
	public void manyFiles() throws IOException {
		List<File> testFiles = new ArrayList<>();
		for (int i = 0; i < 100; ++i) {
			testFiles.add(createTestFile(Integer.toString(i) + ".txt", "1\r\n2\r\n"));
		}
		FormatTask task = create(testFiles);
		assertTaskFailure(task,
				"    0.txt",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    1.txt",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    2.txt",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    3.txt",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    4.txt",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    5.txt",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    6.txt",
				"    @@ -1,2 +1,2 @@",
				"    -1\\r\\n",
				"    -2\\r\\n",
				"    +1\\n",
				"    +2\\n",
				"    Problems in 93 additional file(s)",
				"");
	}
}
