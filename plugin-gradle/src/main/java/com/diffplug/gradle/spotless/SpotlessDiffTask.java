/*
 * Copyright 2016-2024 DiffPlug
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
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.spotless.Formatter;

/**
 * Task for generating diffs in ReviewDog format
 */
public abstract class SpotlessDiffTask extends DefaultTask {
	private final Formatter formatter;
	private FileCollection sources;
	private File outputFile;

	public SpotlessDiffTask(Formatter formatter) {
		this.formatter = formatter;
		setGroup("verification");
		setDescription("Generates diffs between original and formatted code for ReviewDog");
	}

	@InputFiles
	public FileCollection getSources() {
		return sources;
	}

	public void setSources(FileCollection sources) {
		this.sources = sources;
	}

	@OutputFile
	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@TaskAction
	public void generateDiff() {
		try {
			List<File> files = sources.getFiles().stream()
				.filter(File::isFile)
				.collect(Collectors.toList());

			SpotlessDiff spotlessDiff = new SpotlessDiff(formatter);
			String diffs = spotlessDiff.generateDiffs(files);

			if (!outputFile.getParentFile().exists()) {
				outputFile.getParentFile().mkdirs();
			}

			Files.writeString(outputFile.toPath(), diffs);

			getLogger().lifecycle("Generated ReviewDog compatible diff at: {}",
				outputFile.getAbsolutePath());

		} catch (IOException e) {
			throw new GradleException("Failed to generate diff", e);
		}
	}
}
