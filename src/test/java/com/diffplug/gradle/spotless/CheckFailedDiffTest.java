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

import java.io.IOException;
import java.util.Collections;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Throwing.Function;

public class CheckFailedDiffTest extends ResourceHarness {
	private Project project;
	private FormatTask task;

	@Before
	public void createTask() {
		project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		task = project.getTasks().create("underTest", FormatTask.class);
		task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		task.check = true;
	}

	@SafeVarargs
	private final void testCase(String input, String output, Function<String, String>... formatters) throws IOException {
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		FormatTask task = project.getTasks().create("underTest", FormatTask.class);
		task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
		task.check = true;
		task.target = Collections.singleton(createTestFile("testFile", input));
		for (Function<String, String> formatter : formatters) {
			task.steps.add(FormatterStep.create("test", formatter));
		}

		try {
			task.execute();
		} catch (TaskExecutionException e) {
			GradleException cause = (GradleException) e.getCause();
			String msg = cause.getMessage();
			String firstLine = "The following files had format violations:\n";
			String lastLine = "\nFormat violations were found. Run 'gradlew spotlessApply' to fix them.";
			Assertions.assertThat(msg).startsWith(firstLine).endsWith(lastLine);
			String middle = msg.substring(firstLine.length(), msg.length() - lastLine.length());
			Assertions.assertThat(middle).isEqualTo(output.substring(0, output.length() - 1));
		}
	}

	@Test
	public void lineEndingProblem() throws IOException {
		testCase("A\r\nB\r\nC\r\n", StringPrinter.buildStringFromLines(
				"  testFile",
				"    @@ -1,3 +1,3 @@",
				"    -A\\r\\n",
				"    -B\\r\\n",
				"    -C\\r\\n",
				"    +A\\n",
				"    +B\\n",
				"    +C\\n"));
	}

	@Test
	public void whitespaceProblem() throws IOException {
		testCase("A \nB\t\nC  \n", StringPrinter.buildStringFromLines(
				"  testFile",
				"    @@ -1,3 +1,3 @@",
				"    -A·",
				"    -B\\t",
				"    -C··",
				"    +A",
				"    +B",
				"    +C"), input -> {
					Pattern pattern = Pattern.compile("[ \t]+$", Pattern.UNIX_LINES | Pattern.MULTILINE);
					return pattern.matcher(input).replaceAll("");
				});
	}
}
