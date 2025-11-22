/*
 * Copyright 2016-2025 DiffPlug
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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.TaskProvider;
import org.junit.jupiter.api.Test;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.NeverUpToDateStep;
import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.SerializedFunction;
import com.diffplug.spotless.TestProvisioner;

class PaddedCellTaskTest extends ResourceHarness {
	private class Bundle {
		String name;
		Project project = TestProvisioner.gradleProject(rootFolder());
		Provider<SpotlessTaskService> taskService = GradleIntegrationHarness.providerOf(new SpotlessTaskService() {
			@Override
			public BuildServiceParameters.None getParameters() {
				return null;
			}
		});
		File file;
		Provider<File> outputFile;
		TaskProvider<SpotlessTaskImpl> source;
		TaskProvider<SpotlessCheck> check;
		TaskProvider<SpotlessApply> apply;

		Bundle(String name, SerializedFunction<String, String> function) throws IOException {
			this.name = name;
			file = setFile("src/test." + name).toContent("CCC");
			FormatterStep step = NeverUpToDateStep.create(name, function);
			source = registerFormatTask(name, step);
			check = registerCheckTask(name, source);
			apply = registerApplyTask(name, source);
			outputFile = source.map(task -> new File(task.getCleanDirectory() + "/src", file.getName()));
		}

		private TaskProvider<SpotlessTaskImpl> registerFormatTask(String name, FormatterStep step) {
			return project.getTasks().register("spotless" + SpotlessPlugin.capitalize(name), SpotlessTaskImpl.class, task -> {
				task.init(taskService);
				task.setSteps(List.of(step));
				task.setLineEndingsPolicy(project.provider(LineEnding.UNIX::createPolicy));
				task.setTarget(singletonList(file));
			});
		}

		private TaskProvider<SpotlessCheck> registerCheckTask(String name, TaskProvider<SpotlessTaskImpl> source) {
			return project.getTasks().register("spotless" + SpotlessPlugin.capitalize(name) + "Check", SpotlessCheck.class, task -> task.init(source));
		}

		private TaskProvider<SpotlessApply> registerApplyTask(String name, TaskProvider<SpotlessTaskImpl> source) {
			return project.getTasks().register("spotless" + SpotlessPlugin.capitalize(name) + "Apply", SpotlessApply.class, task -> task.init(source));
		}

		String checkFailureMsg() {
			try {
				check();
				throw new AssertionError();
			} catch (Exception e) {
				return e.getMessage();
			}
		}

		void diagnose() throws IOException {
			TaskProvider<SpotlessDiagnoseTask> diagnose = project.getTasks().register("spotless" + SpotlessPlugin.capitalize(name) + "Diagnose", SpotlessDiagnoseTask.class);
			diagnose.get().source = source;
			diagnose.get().performAction();
		}

		void format() throws Exception {
			Tasks.execute(source.get());
		}

		void apply() throws Exception {
			Tasks.execute(source.get());
			apply.get().performAction();
		}

		void check() throws Exception {
			Tasks.execute(source.get());
			check.get().performActionTest();
		}
	}

	private Bundle wellbehaved() throws IOException {
		return new Bundle("wellbehaved", x -> "42");
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
	void paddedCellFormat() throws Exception {
		Bundle wellbehaved = wellbehaved();
		Bundle cycle = cycle();
		Bundle converge = converge();
		Bundle diverge = diverge();

		wellbehaved.format();
		cycle.format();
		converge.format();
		diverge.format();

		assertFile(wellbehaved.outputFile.get()).hasContent("42");    // cycle -> first element in cycle
		assertFile(cycle.outputFile.get()).hasContent("A");        // cycle -> first element in cycle
		assertFile(converge.outputFile.get()).hasContent("");    // converge -> converges
		assertThat(diverge.outputFile.get()).doesNotExist();    // diverge -> no change
	}

	@Test
	void paddedCellApplyCheck() throws Exception {
		Bundle wellbehaved = wellbehaved();
		Bundle cycle = cycle();
		Bundle converge = converge();
		Bundle diverge = diverge();

		wellbehaved.apply();
		cycle.apply();
		converge.apply();
		diverge.apply();

		assertFile(wellbehaved.file).hasContent("42");    // cycle -> first element in cycle
		assertFile(cycle.file).hasContent("A");        // cycle -> first element in cycle
		assertFile(converge.file).hasContent("");    // converge -> converges
		assertFile(diverge.file).hasContent("CCC");    // diverge -> no change

		// After apply, check should pass
		wellbehaved.check();
		cycle.check();
		converge.check();
		diverge.check();
	}

	@Test
	void diagnose() throws Exception {
		wellbehaved().diagnose();
		cycle().diagnose();
		converge().diagnose();
		diverge().diagnose();

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

	private void assertFolderContents(String subfolderName, String... files) throws IOException {
		File subfolder = new File(rootFolder(), subfolderName);
		assertTrue(subfolder.isDirectory());
		String asList = String.join("\n", Arrays.asList(files));
		assertEquals(StringPrinter.buildStringFromLines(files).trim(), asList);
	}

	@Test
	void paddedCellCheckCycleFailureMsg() throws IOException {
		assertFailureMessage(cycle(),
				"The following files had format violations:",
				FileSignature.pathUnixToNative("    src/test.cycle"),
				"        @@ -1 +1 @@",
				"        -CCC",
				"        +A",
				DiffMessageFormatterTest.EXPECTED_RUN_SPOTLESS_APPLY_SUGGESTION);
	}

	@Test
	void paddedCellCheckConvergeFailureMsg() throws IOException {
		assertFailureMessage(converge(),
				"The following files had format violations:",
				FileSignature.pathUnixToNative("    src/test.converge"),
				"        @@ -1 +0,0 @@",
				"        -CCC",
				DiffMessageFormatterTest.EXPECTED_RUN_SPOTLESS_APPLY_SUGGESTION);
	}

	private void assertFailureMessage(Bundle bundle, String... expectedOutput) {
		String msg = bundle.checkFailureMsg();
		String expected = StringPrinter.buildStringFromLines(expectedOutput).trim();
		assertEquals(expected, msg);
	}
}
