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
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Throwing;

public class PaddedCellTaskTest extends ResourceHarness {
	class Bundle {
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		File file;
		FormatTask check, apply;

		Bundle(String name, Throwing.Function<String, String> function) throws IOException {
			file = createTestFile("test." + name, "CCC");
			FormatterStep step = FormatterStep.create(name, function);
			check = create(name, step, true);
			apply = create(name, step, false);
		}

		@SuppressWarnings("deprecation")
		private FormatTask create(String name, FormatterStep step, boolean check) {
			FormatTask task = project.getTasks().create("spotless" + SpotlessPlugin.capitalize(name) + (check ? "Check" : "Apply"), FormatTask.class);
			task.steps.add(step);
			task.lineEndingsPolicy = LineEnding.UNIX.createPolicy();
			task.check = check;
			task.target = Collections.singletonList(file);
			return task;
		}

		private Bundle paddedCell() {
			check.paddedCell = true;
			apply.paddedCell = true;
			return this;
		}

		private String checkFailureMsg() {
			try {
				check.execute();
				throw new AssertionError();
			} catch (TaskExecutionException e) {
				GradleException cause = (GradleException) e.getCause();
				return cause.getMessage();
			}
		}
	}

	private Bundle cycle() throws IOException {
		return new Bundle("cycle", x -> x.equals("A") ? "B" : "A");
	}

	private Bundle converge() throws IOException {
		return new Bundle("converge", x -> x.isEmpty() ? x : x.substring(0, x.length() - 1));
	}

	private Bundle diverge() throws IOException {
		return new Bundle("diverge", x -> x + " ");
	}

	@Test
	public void failsWithoutPaddedCell() throws IOException {
		Assertions.assertThat(cycle().checkFailureMsg()).startsWith("You have a misbehaving rule");
		Assertions.assertThat(converge().checkFailureMsg()).startsWith("You have a misbehaving rule");
		Assertions.assertThat(diverge().checkFailureMsg()).startsWith("You have a misbehaving rule");
	}

	@Test
	public void paddedCellApply() throws IOException {
		Bundle cycle = cycle().paddedCell();
		Bundle converge = converge().paddedCell();
		Bundle diverge = diverge().paddedCell();

		cycle.apply.execute();
		converge.apply.execute();
		diverge.apply.execute();

		assertFileContent("A", cycle.file);		// cycle -> first element in cycle
		assertFileContent("", converge.file);	// converge -> converges
		assertFileContent("CCC", diverge.file);	// diverge -> no change
	}

	@Test
	public void paddedCellCheckFailureFiles() throws Throwable {
		cycle().paddedCell().checkFailureMsg();
		converge().paddedCell().checkFailureMsg();
		diverge().paddedCell().check.execute();

		assertFolderContents("build/spotless-diagnose-cycle",
				"test.cycle.cycle0",
				"test.cycle.cycle1");
		assertFolderContents("build/spotless-diagnose-converge",
				"test.converge.converge0",
				"test.converge.converge1",
				"test.converge.converge2");
		assertFolderContents("build/spotless-diagnose-diverge",
				"test.diverge.diverge0",
				"test.diverge.diverge1",
				"test.diverge.diverge2",
				"test.diverge.diverge3",
				"test.diverge.diverge4",
				"test.diverge.diverge5",
				"test.diverge.diverge6",
				"test.diverge.diverge7",
				"test.diverge.diverge8",
				"test.diverge.diverge9");
	}

	private void assertFolderContents(String subfolderName, String... files) {
		File subfolder = new File(folder.getRoot(), subfolderName);
		String asList = Arrays.asList(subfolder.list()).stream().collect(Collectors.joining("\n"));
		Assert.assertEquals(StringPrinter.buildStringFromLines(files).trim(), asList);
	}

	@Test
	public void paddedCellCheckCycleFailureMsg() throws Throwable {
		assertFailureMessage(cycle().paddedCell(),
				"The following files had format violations:",
				"    test.cycle",
				"    @@ -1 +1 @@",
				"    -CCC",
				"    +A",
				"Run 'gradlew spotlessApply' to fix these violations.");
	}

	@Test
	public void paddedCellCheckConvergeFailureMsg() throws Throwable {
		assertFailureMessage(converge().paddedCell(),
				"The following files had format violations:",
				"    test.converge",
				"    @@ -1 +0,0 @@",
				"    -CCC",
				"Run 'gradlew spotlessApply' to fix these violations.");
	}

	private void assertFailureMessage(Bundle bundle, String... expectedOutput) throws Throwable {
		String msg = bundle.checkFailureMsg();
		String expected = StringPrinter.buildStringFromLines(expectedOutput).trim();
		Assert.assertEquals(expected, msg);
	}
}
