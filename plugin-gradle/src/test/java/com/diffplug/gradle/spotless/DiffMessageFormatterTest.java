/*
 * Copyright 2016-2022 DiffPlug
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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.assertj.core.api.Assertions;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceParameters;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.TestProvisioner;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

class DiffMessageFormatterTest extends ResourceHarness {

	private class Bundle {
		Project project = TestProvisioner.gradleProject(rootFolder());
		Provider<SpotlessTaskService> taskService = GradleIntegrationHarness.providerOf(new SpotlessTaskService() {
			@Override
			public BuildServiceParameters.None getParameters() {
				return null;
			}
		});

		File file;
		SpotlessTaskImpl task;
		SpotlessCheck check;

		Bundle(String name) throws IOException {
			file = setFile("src/test." + name).toContent("CCC");
			task = createFormatTask(name);
			check = createCheckTask(name, task);
			createApplyTask(name, task);
		}

		private SpotlessTaskImpl createFormatTask(String name) {
			SpotlessTaskImpl task = project.getTasks().create("spotless" + SpotlessPlugin.capitalize(name), SpotlessTaskImpl.class);
			task.init(taskService);
			task.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
			task.setTarget(Collections.singletonList(file));
			return task;
		}

		private SpotlessCheck createCheckTask(String name, SpotlessTaskImpl source) {
			SpotlessCheck task = project.getTasks().create("spotless" + SpotlessPlugin.capitalize(name) + "Check", SpotlessCheck.class);
			task.init(source);
			return task;
		}

		private SpotlessApply createApplyTask(String name, SpotlessTaskImpl source) {
			SpotlessApply task = project.getTasks().create("spotless" + SpotlessPlugin.capitalize(name) + "Apply", SpotlessApply.class);
			task.init(source);
			return task;
		}

		String checkFailureMsg() {
			try {
				check();
				throw new AssertionError();
			} catch (Exception e) {
				return e.getMessage();
			}
		}

		void check() throws Exception {
			Tasks.execute(task);
			check.performActionTest();
		}
	}

	private Bundle create(File... files) throws IOException {
		return create(Arrays.asList(files));
	}

	private Bundle create(List<File> files) throws IOException {
		Bundle bundle = new Bundle("underTest");
		bundle.task.setLineEndingsPolicy(LineEnding.UNIX.createPolicy());
		bundle.task.setTarget(files);
		return bundle;
	}

	private void assertCheckFailure(Bundle spotless, String... expectedLines) throws Exception {
		String msg = spotless.checkFailureMsg();

		String firstLine = "The following files had format violations:\n";
		String lastLine = "\n" + EXPECTED_RUN_SPOTLESS_APPLY_SUGGESTION;
		Assertions.assertThat(msg).startsWith(firstLine).endsWith(lastLine);

		String middle = msg.substring(firstLine.length(), msg.length() - lastLine.length());
		String expectedMessage = StringPrinter.buildStringFromLines(expectedLines);
		Assertions.assertThat(middle).isEqualTo(expectedMessage.substring(0, expectedMessage.length() - 1));
	}

	static final String EXPECTED_RUN_SPOTLESS_APPLY_SUGGESTION = FileSignature.machineIsWin()
			? "Run 'gradlew.bat :spotlessApply' to fix these violations."
			: "Run './gradlew :spotlessApply' to fix these violations.";

	@Test
	void lineEndingProblem() throws Exception {
		Bundle task = create(setFile("testFile").toContent("A\r\nB\r\nC\r\n"));
		assertCheckFailure(task,
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
	void customRunToFixMessage() throws Exception {
		Bundle task = create(setFile("testFile").toContent("A\r\nB\r\nC\r\n"));
		String customMessage = "Formatting issues detected, please read automatic-code-formatting.txt and correct.";
		task.check.getRunToFixMessage().set(customMessage);

		String msg = task.checkFailureMsg();

		String firstLine = "The following files had format violations:\n";
		String lastLine = "\n" + customMessage;
		Assertions.assertThat(msg).startsWith(firstLine).endsWith(lastLine);
	}

	@Test
	void whitespaceProblem() throws Exception {
		Bundle spotless = create(setFile("testFile").toContent("A \nB\t\nC  \n"));
		spotless.task.addStep(FormatterStep.createNeverUpToDate("trimTrailing", input -> {
			Pattern pattern = Pattern.compile("[ \t]+$", Pattern.UNIX_LINES | Pattern.MULTILINE);
			return pattern.matcher(input).replaceAll("");
		}));
		assertCheckFailure(spotless,
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
	void multipleFiles() throws Exception {
		Bundle spotless = create(
				setFile("A").toContent("1\r\n2\r\n"),
				setFile("B").toContent("3\n4\r\n"));
		assertCheckFailure(spotless,
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
	void manyFiles() throws Exception {
		List<File> testFiles = new ArrayList<>();
		for (int i = 0; i < 9 + DiffMessageFormatter.MAX_FILES_TO_LIST - 1; ++i) {
			String fileName = String.format("%02d", i) + ".txt";
			testFiles.add(setFile(fileName).toContent("1\r\n2\r\n"));
		}
		Bundle spotless = create(testFiles);
		assertCheckFailure(spotless,
				"    00.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    01.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    02.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    03.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    04.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    05.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    06.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    07.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    08.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"    ... (2 more lines that didn't fit)",
				"Violations also present in:",
				"    09.txt",
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
	void manyManyFiles() throws Exception {
		List<File> testFiles = new ArrayList<>();
		for (int i = 0; i < 9 + DiffMessageFormatter.MAX_FILES_TO_LIST; ++i) {
			String fileName = String.format("%02d", i) + ".txt";
			testFiles.add(setFile(fileName).toContent("1\r\n2\r\n"));
		}
		Bundle spotless = create(testFiles);
		assertCheckFailure(spotless,
				"    00.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    01.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    02.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    03.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    04.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    05.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    06.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    07.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"        +1\\n",
				"        +2\\n",
				"    08.txt",
				"        @@ -1,2 +1,2 @@",
				"        -1\\r\\n",
				"        -2\\r\\n",
				"    ... (2 more lines that didn't fit)",
				"Violations also present in " + DiffMessageFormatter.MAX_FILES_TO_LIST + " other files.");
	}

	@Test
	void longFile() throws Exception {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 1000; ++i) {
			builder.append(i);
			builder.append("\r\n");
		}
		Bundle spotless = create(setFile("testFile").toContent(builder.toString()));
		assertCheckFailure(spotless,
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
