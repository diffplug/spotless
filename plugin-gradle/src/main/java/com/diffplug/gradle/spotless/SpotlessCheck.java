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

import com.diffplug.spotless.FileSignature;
import com.diffplug.spotless.ThrowingEx;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

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
		ConfigurableFileTree files = getConfigCacheWorkaround().fileTree().from(getSpotlessOutDirectory().get());
		if (files.isEmpty()) {
			getState().setDidWork(sourceDidWork());
		} else if (!isTest && applyHasRun()) {
			// if our matching apply has already run, then we don't need to do anything
			getState().setDidWork(false);
		} else {
			List<File> problemFiles = new ArrayList<>();
			files.visit(new FileVisitor() {
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
						// but it's very erratic, and that test writes both to gradle cache
						// and git cache very quickly.  Either of gradle or jgit might be
						// caching something wrong because of the fast repeated writes.
						if (!Arrays.equals(userFile, formatted)) {
							// If the on-disk content is equal to the formatted content,
							// just don't add it as a problem file. Easy!
							problemFiles.add(originalSource);
						}
					} catch (IOException e) {
						throw ThrowingEx.asRuntime(e);
					}
				}
			});
			if (!problemFiles.isEmpty()) {
				Collections.sort(problemFiles);
				throw new GradleException(DiffMessageFormatter.builder()
						.runToFix(getRunToFixMessage().get())
						.formatterFolder(
								getProjectDir().get().getAsFile().toPath(),
								getSpotlessOutDirectory().get().toPath(),
								getEncoding().get())
						.problemFiles(problemFiles)
						.getMessage());
			}
		}
	}

	@Internal
	abstract Property<String> getProjectPath();

	@Override
	void init(SpotlessTaskImpl impl) {
		super.init(impl);
		getProjectPath().set(getProject().getPath());
		getEncoding().set(impl.getEncoding());
		getRunToFixMessage().convention(
				"Run '" + calculateGradleCommand() + " " + getTaskPathPrefix() + "spotlessApply' to fix these violations.");
	}

	private String getTaskPathPrefix() {
		String path = getProjectPath().get();
		return path.equals(":") ? ":" : path + ":";
	}

	private static String calculateGradleCommand() {
		return FileSignature.machineIsWin() ? "gradlew.bat" : "./gradlew";
	}
}
