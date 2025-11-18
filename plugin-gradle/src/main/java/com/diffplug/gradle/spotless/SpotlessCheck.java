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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.work.DisableCachingByDefault;
import org.jetbrains.annotations.NotNull;

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

@DisableCachingByDefault(because = "not worth caching")
public abstract class SpotlessCheck extends SpotlessTaskService.ClientTask {
	@Internal
	public abstract Property<String> getEncoding();

	@Input
	public abstract Property<String> getRunToFixMessage();

	public void performActionTest() throws IOException {
		performAction(true);
	}

	@TaskAction
	public void performAction() throws IOException {
		performAction(false);
	}

	private void performAction(boolean isTest) throws IOException {
		ConfigurableFileTree cleanFiles = getConfigCacheWorkaround().fileTree().from(getSpotlessCleanDirectory().get());
		ConfigurableFileTree lintsFiles = getConfigCacheWorkaround().fileTree().from(getSpotlessLintsDirectory().get());
		if (cleanFiles.isEmpty() && lintsFiles.isEmpty()) {
			getState().setDidWork(sourceDidWork());
		} else if (!isTest && applyHasRun()) {
			// if our matching apply has already run, then we don't need to do anything
			getState().setDidWork(false);
		} else {
			List<File> unformattedFiles = getUncleanFiles(cleanFiles);
			if (!unformattedFiles.isEmpty()) {
				// if any files are unformatted, we show those
				throw new GradleException(DiffMessageFormatter.builder()
						.runToFix(getRunToFixMessage().get())
						.formatterFolder(
								getProjectDir().get().getAsFile().toPath(),
								getSpotlessCleanDirectory().get().toPath(),
								getEncoding().get())
						.problemFiles(unformattedFiles)
						.getMessage());
			} else {
				// We only show lints if there are no unformatted files.
				// This is because lint line numbers are relative to the
				// formatted content, and formatting often fixes lints.
				boolean detailed = false;
				throw new GradleException(super.allLintsErrorMsgDetailed(lintsFiles, detailed));
			}
		}
	}

	private @NotNull List<File> getUncleanFiles(ConfigurableFileTree cleanFiles) {
		List<File> uncleanFiles = new ArrayList<>();
		cleanFiles.visit(new FileVisitor() {
			@Override
			public void visitDir(FileVisitDetails fileVisitDetails) {

			}

			@Override
			public void visitFile(FileVisitDetails fileVisitDetails) {
				String path = fileVisitDetails.getPath();
				File originalSource = new File(getProjectDir().get().getAsFile(), path);
				try {
					// read the file on disk
					byte[] userFile = Files.readAllBytes(originalSource.toPath());
					// and the formatted version from spotlessOutDirectory
					byte[] formatted;
					{
						ByteArrayOutputStream clean = new ByteArrayOutputStream();
						fileVisitDetails.copyTo(clean);
						formatted = clean.toByteArray();
					}
					// If these two are equal, it means that SpotlessTask left a file
					// in its output directory which ought to have been removed. As
					// best I can tell, this is a filesytem race which is very hard
					// to trigger.  GitRatchetGradleTest can *sometimes* reproduce it
					// but it's very erratic, and that test writes both to Gradle cache
					// and git cache very quickly.  Either of Gradle or jgit might be
					// caching something wrong because of the fast repeated writes.
					if (!Arrays.equals(userFile, formatted)) {
						// If the on-disk content is equal to the formatted content,
						// just don't add it as a problem file. Easy!
						uncleanFiles.add(originalSource);
					}
				} catch (IOException e) {
					throw ThrowingEx.asRuntime(e);
				}
			}
		});
		Collections.sort(uncleanFiles);
		return uncleanFiles;
	}

	@Internal
	abstract Property<String> getProjectPath();

	@Override
	void init(TaskProvider<SpotlessTaskImpl> impl) {
		super.init(impl);
		getProjectPath().set(getProject().getPath());
		getEncoding().set(impl.map(SpotlessTask::getEncoding));
		getRunToFixMessage().convention(
				"Run '" + calculateGradleCommand() + " spotlessApply' to fix all violations.");
	}

	private static String calculateGradleCommand() {
		return FileSignature.machineIsWin() ? "gradlew.bat" : "./gradlew";
	}
}
