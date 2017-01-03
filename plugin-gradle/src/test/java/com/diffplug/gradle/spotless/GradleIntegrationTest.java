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
import java.util.List;
import java.util.ListIterator;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Assert;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.tree.TreeDef;
import com.diffplug.common.tree.TreeStream;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.ResourceHarness;

public class GradleIntegrationTest extends ResourceHarness {
	protected GradleRunner gradleRunner() throws IOException {
		return GradleRunner.create().withProjectDir(rootFolder()).withPluginClasspath().forwardOutput();
	}

	/** Dumps the complete file contents of the folder to the console. */
	protected String getContents() throws IOException {
		return getContents(subPath -> !subPath.startsWith(".gradle"));
	}

	protected String getContents(Predicate<String> subpathsToInclude) throws IOException {
		TreeDef<File> treeDef = TreeDef.forFile(Errors.rethrow());
		List<File> files = TreeStream.depthFirst(treeDef, rootFolder())
				.filter(File::isFile)
				.collect(Collectors.toList());

		ListIterator<File> iterator = files.listIterator(files.size());
		int rootLength = rootFolder().getAbsolutePath().length() + 1;
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

	protected void applyIsUpToDate(boolean upToDate) throws IOException {
		taskIsUpToDate("spotlessApply", upToDate);
	}

	protected void checkIsUpToDate(boolean upToDate) throws IOException {
		taskIsUpToDate("spotlessCheck", upToDate);
	}

	private static final boolean IS_UNIX = LineEnding.PLATFORM_NATIVE.str().equals("\n");
	private static final int FILESYSTEM_RESOLUTION_MS = IS_UNIX ? 2000 : 150;

	void pauseForFilesystem() {
		Errors.rethrow().run(() -> Thread.sleep(FILESYSTEM_RESOLUTION_MS));
	}

	private void taskIsUpToDate(String task, boolean upToDate) throws IOException {
		pauseForFilesystem();
		BuildResult buildResult = gradleRunner().withArguments(task).build();

		TaskOutcome expected = upToDate ? TaskOutcome.UP_TO_DATE : TaskOutcome.SUCCESS;
		TaskOutcome notExpected = upToDate ? TaskOutcome.SUCCESS : TaskOutcome.UP_TO_DATE;

		boolean everythingAsExpected = !buildResult.tasks(expected).isEmpty() &&
				buildResult.tasks(notExpected).isEmpty() &&
				buildResult.getTasks().size() == buildResult.tasks(expected).size();
		if (!everythingAsExpected) {
			Assert.fail("Expected all tasks to be " + expected + ", but instead was\n" + buildResultToString(buildResult));
		}
	}

	static String buildResultToString(BuildResult result) {
		return StringPrinter.buildString(printer -> {
			for (BuildTask task : result.getTasks()) {
				printer.println(task.getPath() + " " + task.getOutcome());
			}
		});
	}
}
