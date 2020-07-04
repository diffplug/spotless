/*
 * Copyright 2016-2020 DiffPlug
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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.common.base.Throwing;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;

@CacheableTask
public class SpotlessTask extends SpotlessTaskBase {
	protected String filePatterns = "";

	@Input
	public String getFilePatterns() {
		return filePatterns;
	}

	public void setFilePatterns(String filePatterns) {
		if (!filePatterns.equals("") && this instanceof SpotlessTaskModern) {
			throw new IllegalArgumentException("-PspotlessFiles is not supported in the modern plugin");
		}
		this.filePatterns = Objects.requireNonNull(filePatterns);
	}

	@TaskAction
	public void performAction(IncrementalTaskInputs inputs) throws Exception {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> target'");
		}

		if (!inputs.isIncremental()) {
			getLogger().info("Not incremental: removing prior outputs");
			getProject().delete(outputDirectory);
			Files.createDirectories(outputDirectory.toPath());
		}

		Throwing.Specific.Predicate<File, IOException> shouldInclude;
		if (this.filePatterns.isEmpty()) {
			shouldInclude = file -> true;
		} else {
			Preconditions.checkArgument(ratchet == null,
					"Cannot use 'ratchetFrom' and '-PspotlessFiles' at the same time");

			// a list of files has been passed in via project property
			final String[] includePatterns = this.filePatterns.split(",");
			final List<Pattern> compiledIncludePatterns = Arrays.stream(includePatterns)
					.map(Pattern::compile)
					.collect(Collectors.toList());
			shouldInclude = file -> compiledIncludePatterns
					.stream()
					.anyMatch(filePattern -> filePattern.matcher(file.getAbsolutePath())
							.matches());
		}

		try (Formatter formatter = buildFormatter()) {
			inputs.outOfDate(inputDetails -> {
				File input = inputDetails.getFile();
				try {
					if (shouldInclude.test(input) && input.isFile()) {
						processInputFile(formatter, input);
					}
				} catch (IOException e) {
					throw Errors.asRuntime(e);
				}
			});
		}

		inputs.removed(removedDetails -> {
			File input = removedDetails.getFile();
			try {
				if (shouldInclude.test(input)) {
					deletePreviousResult(input);
				}
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		});
	}

	protected void processInputFile(Formatter formatter, File input) throws IOException {
		File output = getOutputFile(input);
		getLogger().debug("Applying format to " + input + " and writing to " + output);
		PaddedCell.DirtyState dirtyState;
		if (ratchet != null && ratchet.isClean(getProject(), rootTreeSha, input)) {
			dirtyState = PaddedCell.isClean();
		} else {
			dirtyState = PaddedCell.calculateDirtyState(formatter, input);
		}
		if (dirtyState.isClean()) {
			// Remove previous output if it exists
			Files.deleteIfExists(output.toPath());
		} else if (dirtyState.didNotConverge()) {
			getLogger().warn("Skipping '" + input + "' because it does not converge.  Run `spotlessDiagnose` to understand why");
		} else {
			Path parentDir = output.toPath().getParent();
			if (parentDir == null) {
				throw new IllegalStateException("Every file has a parent folder.");
			}
			Files.createDirectories(parentDir);
			dirtyState.writeCanonicalTo(output);
		}
	}

	protected void deletePreviousResult(File input) throws IOException {
		File output = getOutputFile(input);
		if (output.isDirectory()) {
			Files.walk(output.toPath())
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		} else {
			Files.deleteIfExists(output.toPath());
		}
	}

	private File getOutputFile(File input) {
		String outputFileName = FormatExtension.relativize(getProject().getProjectDir(), input);
		if (outputFileName == null) {
			throw new IllegalArgumentException(StringPrinter.buildString(printer -> {
				printer.println("Spotless error! All target files must be within the project root. In project " + getProject().getPath());
				printer.println("  root dir: " + getProject().getProjectDir().getAbsolutePath());
				printer.println("    target: " + input.getAbsolutePath());
			}));
		}
		return new File(outputDirectory, outputFileName);
	}
}
