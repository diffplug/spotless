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
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Assert;
import org.junit.Test;

import com.diffplug.common.base.StandardSystemProperty;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Throwing;

public class PaddedCellTaskTest extends ResourceHarness {
	private static final boolean IS_WIN = StandardSystemProperty.OS_NAME.value().toLowerCase(Locale.US).contains("win");

	private static String slashify(String input) {
		return IS_WIN ? input.replace('/', '\\') : input;
	}

	private class Bundle {
		Project project = ProjectBuilder.builder().withProjectDir(folder.getRoot()).build();
		File file;
		CheckFormatTask check;
		ApplyFormatTask apply;

		Bundle(String name, Throwing.Function<String, String> function) throws IOException {
			file = createTestFile("src/test." + name, "CCC");
			FormatterStep step = FormatterStep.create(name, function);
			check = createCheckTask(name, step);
			apply = createApplyTask(name, step);
		}

		private CheckFormatTask createCheckTask(String name, FormatterStep step) {
			CheckFormatTask task = project.getTasks().create("spotless" + SpotlessPlugin.capitalize(name) + SpotlessPlugin.CHECK, CheckFormatTask.class);
			task.steps.add(step);
			task.lineEndingPolicy = LineEnding.UNIX.createPolicy();
			task.target = Collections.singletonList(file);
			return task;
		}

		private ApplyFormatTask createApplyTask(String name, FormatterStep step) {
			ApplyFormatTask task = project.getTasks().create("spotless" + SpotlessPlugin.capitalize(name) + SpotlessPlugin.APPLY, ApplyFormatTask.class);
			task.steps.add(step);
			task.lineEndingPolicy = LineEnding.UNIX.createPolicy();
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

		assertFolderContents("build",
				"spotless-diagnose-converge",
				"spotless-diagnose-cycle",
				"spotless-diagnose-diverge");
		assertFolderContents("build/spotless-diagnose-cycle/src",
				"test.cycle.cycle0",
				"test.cycle.cycle1");
		assertFolderContents("build/spotless-diagnose-converge/src",
				"test.converge.converge0",
				"test.converge.converge1",
				"test.converge.converge2");
		assertFolderContents("build/spotless-diagnose-diverge/src",
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
		Assert.assertTrue(subfolder.isDirectory());
		String asList = Arrays.stream(subfolder.list()).sorted().collect(Collectors.joining("\n"));
		Assert.assertEquals(StringPrinter.buildStringFromLines(files).trim(), asList);
	}

	@Test
	public void paddedCellCheckCycleFailureMsg() throws Throwable {
		assertFailureMessage(cycle().paddedCell(),
				"The following files had format violations:",
				slashify("    src/test.cycle"),
				"    @@ -1 +1 @@",
				"    -CCC",
				"    +A",
				"Run 'gradlew spotlessApply' to fix these violations.");
	}

	@Test
	public void paddedCellCheckConvergeFailureMsg() throws Throwable {
		assertFailureMessage(converge().paddedCell(),
				"The following files had format violations:",
				slashify("    src/test.converge"),
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
