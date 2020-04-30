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

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

public class SpotlessCheck extends DefaultTask {
	public SpotlessTask formatTask;

	private File spotlessOutDirectory;

	@InputDirectory
	@SkipWhenEmpty
	public File getSpotlessOutDirectory() {
		return spotlessOutDirectory;
	}

	public void setSpotlessOutDirectory(File spotlessOutDirectory) {
		this.spotlessOutDirectory = spotlessOutDirectory;
	}

	@TaskAction
	public void performAction() throws Exception {
		ConfigurableFileTree files = getProject().fileTree(spotlessOutDirectory);
		if (!files.isEmpty()) {
			throw new GradleException("Run 'gradlew spotlessApply' to fix these violations.");
			/*
			 * TODO: problemFiles is expected to be the "real files"
			 * This error message should now take the "real files" and "error files" as the inputs, rather than
			 * the "real files" and the formatter.
			DiffMessageFormatter.builder()
					.runToFix("Run 'gradlew spotlessApply' to fix these violations.")
					.isPaddedCell(paddedCell)
					.formatter(formatter)
					.problemFiles(problemFiles)
					.getMessage());
					*/
		}
	}
}
