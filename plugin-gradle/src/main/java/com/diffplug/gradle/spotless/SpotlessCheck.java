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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.taskdefs.condition.Os;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

public class SpotlessCheck extends DefaultTask {
	SpotlessTask source;
	private File spotlessOutDirectory;

	@PathSensitive(PathSensitivity.RELATIVE)
	@InputDirectory
	public File getSpotlessOutDirectory() {
		return spotlessOutDirectory;
	}

	public void setSpotlessOutDirectory(File spotlessOutDirectory) {
		this.spotlessOutDirectory = spotlessOutDirectory;
	}

	@TaskAction
	public void performAction() throws Exception {
		ConfigurableFileTree files = getProject().fileTree(spotlessOutDirectory);
		if (files.isEmpty()) {
			getState().setDidWork(source.getDidWork());
		} else {
			List<File> problemFiles = new ArrayList<>();
			files.visit(new FileVisitor() {
				@Override
				public void visitDir(FileVisitDetails fileVisitDetails) {

				}

				@Override
				public void visitFile(FileVisitDetails fileVisitDetails) {
					String path = fileVisitDetails.getPath();
					File originalSource = new File(getProject().getProjectDir(), path);
					problemFiles.add(originalSource);
				}
			});

			if (!problemFiles.isEmpty()) {
				Formatter formatter = source.buildFormatter();
				Collections.sort(problemFiles);
				throw formatViolationsFor(formatter, problemFiles);
			}
		}
	}

	/** Returns an exception which indicates problem files nicely. */
	private GradleException formatViolationsFor(Formatter formatter, List<File> problemFiles) {
		return new GradleException(DiffMessageFormatter.builder()
				.runToFix("Run '" + calculateGradleCommand() + " " + getTaskPathPrefix() + "spotlessApply' to fix these violations.")
				.formatter(formatter)
				.problemFiles(problemFiles)
				.getMessage());
	}

	private String getTaskPathPrefix() {
		return getProject().getPath().equals(":")
			? ":"
			: getProject().getPath() + ":";
	}

	private static String calculateGradleCommand() {
		return Os.isFamily(Os.FAMILY_WINDOWS)
			? "gradlew.bat"
			: "./gradlew";
	}
}
