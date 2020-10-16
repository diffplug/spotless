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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;

@CacheableTask
public abstract class SpotlessTaskImpl extends SpotlessTask {
	final File projectDir;

	@Internal
	public File getProjectDir() {
		return projectDir;
	}

	Formatter buildFormatter() {
		// <sketchy configuration cache trick>
		SpotlessTaskImpl original = SpotlessTaskService.instance().get(getPath());
		if (original == this) {
			// a SpotlessTask is registered with the SpotlessTaskService **only** if configuration ran
			// so if we're in this block, it means that we were configured
			return Formatter.builder()
					.lineEndingsPolicy(lineEndingsPolicy)
					.encoding(Charset.forName(encoding))
					.rootDir(getProjectDir().toPath())
					.steps(steps)
					.exceptionPolicy(exceptionPolicy)
					.build();
		} else {
			// if we're in this block, it means that configuration did not run, and this
			// task was deserialized from disk. All of our fields are ".equals" to their
			// originals, but their transient fields are missing, so we can't actually run
			// them. Luckily, we saved the task from the original, just so that we could restore
			// its formatter, whose transient fields are fully populated.
			return original.buildFormatter();
		}
		// </sketchy configuration cache trick>
	}

	private final FileSystemOperations fileSystemOperations;
	private final ObjectFactory objectFactory;

	@javax.inject.Inject
	public SpotlessTaskImpl(FileSystemOperations fileSystemOperations, ObjectFactory objectFactory) {
		this.fileSystemOperations = fileSystemOperations;
		this.objectFactory = objectFactory;
		this.projectDir = getProject().getProjectDir();
		SpotlessTaskService.instance().put(this);
	}

	ConfigurableFileTree outputFiles() {
		return objectFactory.fileTree().from(getOutputDirectory());
	}

	@TaskAction
	public void performAction(InputChanges inputs) throws Exception {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> target'");
		}

		if (!inputs.isIncremental()) {
			getLogger().info("Not incremental: removing prior outputs");
			fileSystemOperations.delete(spec -> spec.delete(outputDirectory));
			Files.createDirectories(outputDirectory.toPath());
		}

		try (Formatter formatter = buildFormatter()) {
			for (FileChange fileChange : inputs.getFileChanges(target)) {
				File input = fileChange.getFile();
				if (fileChange.getChangeType() == ChangeType.REMOVED) {
					deletePreviousResult(input);
				} else {
					if (input.isFile()) {
						processInputFile(formatter, input);
					}
				}
			}
		}
	}

	private void processInputFile(Formatter formatter, File input) throws IOException {
		File output = getOutputFile(input);
		getLogger().debug("Applying format to " + input + " and writing to " + output);
		PaddedCell.DirtyState dirtyState;
		if (ratchet != null && ratchet.isClean(projectDir, rootTreeSha, input)) {
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
			// Need to copy the original file to the tmp location just to remember the file attributes
			Files.copy(input.toPath(), output.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
			dirtyState.writeCanonicalTo(output);
		}
	}

	private void deletePreviousResult(File input) throws IOException {
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
		String outputFileName = FormatExtension.relativize(projectDir, input);
		if (outputFileName == null) {
			throw new IllegalArgumentException(StringPrinter.buildString(printer -> {
				printer.println("Spotless error! All target files must be within the project root. In project " + getProjectPath(this));
				printer.println("  root dir: " + projectDir.getAbsolutePath());
				printer.println("    target: " + input.getAbsolutePath());
			}));
		}
		return new File(outputDirectory, outputFileName);
	}

	static String getProjectPath(Task task) {
		String taskPath = task.getPath();
		int lastColon = taskPath.lastIndexOf(':');
		return lastColon == -1 ? ":" : taskPath.substring(0, lastColon + 1);
	}
}
