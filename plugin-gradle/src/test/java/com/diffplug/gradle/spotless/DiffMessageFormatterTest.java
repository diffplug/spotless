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
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

public class DiffMessageFormatterTest extends ResourceHarness {
	private CheckFormatTask create(File... files) throws IOException {
		return create(Arrays.asList(files));
	}

	private CheckFormatTask create(List<File> files) throws IOException {
		Project project = ProjectBuilder.builder().withProjectDir(rootFolder()).build();
		CheckFormatTask task = project.getTasks().create("underTest", CheckFormatTask.class);
		task.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
		task.setTarget(files);
		return task;
	}

	private void assertTaskFailure(CheckFormatTask task, String... expectedLines) {
		String msg = getTaskErrorMessage(task);

		String firstLine = "The following files had format violations:\n";
		String lastLine = "\nRun 'gradlew spotlessApply' to fix these violations.";
		Assertions.assertThat(msg).startsWith(firstLine).endsWith(lastLine);

		String middle = msg.substring(firstLine.length(), msg.length() - lastLine.length());
		String expectedMessage = StringPrinter.buildStringFromLines(expectedLines);
		Assertions.assertThat(middle).isEqualTo(expectedMessage.substring(0, expectedMessage.length() - 1));
	}

	@Test
	public void lineEndingProblem() throws Exception {
		CheckFormatTask task = create(createTestFile("testFile", "A\r\nB\r\nC\r\n"));
		assertTaskFailure(task,
				"    testFile",
				"        @@ -1,3 +1,3 @@",
				"        -A\\r\\n",
				"        -B\\r\\n",
				"        -C\\r\\n",
				"        +A\\n",
				"        +B\\n",
				"        +C\\n");
	}

	@Test
	public void whitespaceProblem() throws Exception {
		CheckFormatTask task = create(createTestFile("testFile", "A \nB\t\nC  \n"));
		task.addStep(FormatterStep.createNeverUpToDate("trimTrailing", input -> {
			Pattern pattern = Pattern.compile("[ \t]+$", Pattern.UNIX_LINES | Pattern.MULTILINE);
			return pattern.matcher(input).replaceAll("");
		}));
		assertTaskFailure(task,
				"    testFile",
				"        @@ -1,3 +1,3 @@",
				"        -A·",
				"        -B\\t",
				"        -C··",
				"        +A",
				"        +B",
				"        +C");
	}

	@Test
	public void multipleFiles() throws Exception {
		CheckFormatTask task = create(
				createTestFile("A", "1\r\n2\r\n"),
				createTestFile("B", "3\n4\r\n"));
		assertTaskFailure(task,
				"    A",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    B",
				"        @@ -1,2 +1,2 @@",
				"         3\\n",
				"        -4\\r\\n",
				"        +4\\n");
	}

	@Test
	public void manyFiles() throws Exception {
		List<File> testFiles = new ArrayList<>();
		for (int i = 0; i < 9 + DiffMessageFormatter.MAX_FILES_TO_LIST - 1; ++i) {
			testFiles.add(createTestFile(Integer.toString(i) + ".txt", "1\r\n2\r\n"));
		}
		CheckFormatTask task = create(testFiles);
		assertTaskFailure(task,
				"    0.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    1.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    2.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    3.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    4.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    5.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    6.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    7.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    8.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"    ... (2 more lines that didn't fit)",
				"Violations also present in:",
				"    9.txt",
				"    10.txt",
				"    11.txt",
				"    12.txt",
				"    13.txt",
				"    14.txt",
				"    15.txt",
				"    16.txt",
				"    17.txt");
	}

	@Test
	public void manyManyFiles() throws Exception {
		List<File> testFiles = new ArrayList<>();
		for (int i = 0; i < 9 + DiffMessageFormatter.MAX_FILES_TO_LIST; ++i) {
			testFiles.add(createTestFile(Integer.toString(i) + ".txt", "1\r\n2\r\n"));
		}
		CheckFormatTask task = create(testFiles);
		assertTaskFailure(task,
				"    0.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    1.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    2.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    3.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    4.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    5.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    6.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    7.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    8.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"    ... (2 more lines that didn't fit)",
				"Violations also present in " + DiffMessageFormatter.MAX_FILES_TO_LIST + " other files.");
	}

	@Test
	public void longFile() throws Exception {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 1000; ++i) {
			builder.append(i);
			builder.append("\r\n");
		}
		CheckFormatTask task = create(createTestFile("testFile", builder.toString()));
		assertTaskFailure(task,
				"    testFile",
				"        @@ -1,1000 +1,1000 @@",
				"        -0\\r\\n",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        -3\\r\\n",
				"        -4\\r\\n",
				"        -5\\r\\n",
				"        -6\\r\\n",
				"        -7\\r\\n",
				"        -8\\r\\n",
				"        -9\\r\\n",
				"        -10\\r\\n",
				"        -11\\r\\n",
				"        -12\\r\\n",
				"        -13\\r\\n",
				"        -14\\r\\n",
				"        -15\\r\\n",
				"        -16\\r\\n",
				"        -17\\r\\n",
				"        -18\\r\\n",
				"        -19\\r\\n",
				"        -20\\r\\n",
				"        -21\\r\\n",
				"        -22\\r\\n",
				"        -23\\r\\n",
				"        -24\\r\\n",
				"        -25\\r\\n",
				"        -26\\r\\n",
				"        -27\\r\\n",
				"        -28\\r\\n",
				"        -29\\r\\n",
				"        -30\\r\\n",
				"        -31\\r\\n",
				"        -32\\r\\n",
				"        -33\\r\\n",
				"        -34\\r\\n",
				"        -35\\r\\n",
				"        -36\\r\\n",
				"        -37\\r\\n",
				"        -38\\r\\n",
				"        -39\\r\\n",
				"        -40\\r\\n",
				"        -41\\r\\n",
				"        -42\\r\\n",
				"        -43\\r\\n",
				"        -44\\r\\n",
				"        -45\\r\\n",
				"        -46\\r\\n",
				"        -47\\r\\n",
				"    ... (1952 more lines that didn't fit)");
	}
}
