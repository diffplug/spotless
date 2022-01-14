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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;

import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.DirtyState;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.Lint;
import com.diffplug.spotless.extra.GitRatchet;

@CacheableTask
public abstract class SpotlessTaskImpl extends SpotlessTask {
	@Internal
	abstract DirectoryProperty getProjectDir();

	void init(Provider<SpotlessTaskService> service) {
		getTaskService().set(service);
		getProjectDir().set(getProject().getProjectDir());
	}

	@Inject
	protected abstract FileSystemOperations getFs();

	@TaskAction
	public void performAction(InputChanges inputs) throws Exception {
		SpotlessTaskService taskService = getTaskService().get();
		taskService.registerSourceAlreadyRan(this);
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> target'");
		}

		if (!inputs.isIncremental()) {
			getLogger().info("Not incremental: removing prior outputs");
			getFs().delete(d -> d.delete(outputDirectory));
			Files.createDirectories(contentDir().toPath());
			Files.createDirectories(lintApplyDir().toPath());
			Files.createDirectories(lintCheckDir().toPath());
		}

		try (Formatter formatter = buildFormatter()) {
			GitRatchetGradle ratchet = getRatchet();
			for (FileChange fileChange : inputs.getFileChanges(target)) {
				File input = fileChange.getFile();
				if (fileChange.getChangeType() == ChangeType.REMOVED) {
					deletePreviousResult(input);
				} else {
					if (input.isFile()) {
						processInputFile(ratchet, formatter, input);
					}
				}
			}
		}
	}

	private void processInputFile(@Nullable GitRatchet ratchet, Formatter formatter, File input) throws IOException {
		File output = getOutputFile(input);
		getLogger().debug("Applying format to " + input + " and writing to " + output);
		DirtyState dirtyState;
		List<Lint> lintsCheck, lintsApply;
		if (ratchet != null && ratchet.isClean(getProjectDir().get().getAsFile(), getRootTreeSha(), input)) {
			dirtyState = DirtyState.clean();
			lintsCheck = Collections.emptyList();
			lintsApply = Collections.emptyList();
		} else {
			DirtyState.Calculation calculation = DirtyState.of(formatter, input);
			dirtyState = calculation.calculateDirtyState();
			lintsCheck = calculation.calculateLintAgainstRaw();
			lintsApply = calculation.calculateLintAgainstDirtyState(dirtyState, lintsCheck);
		}
		if (dirtyState.isClean()) {
			// Remove previous output if it exists
			Files.deleteIfExists(output.toPath());
		} else if (dirtyState.didNotConverge()) {
			getLogger().warn("Skipping '" + input + "' because it does not converge.  Run {@code spotlessDiagnose} to understand why");
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

		writeLints(lintsCheck, getLintCheckFile(input));
		writeLints(lintsApply, getLintApplyFile(input));
	}

	private void writeLints(List<Lint> lints, File lintFile) throws IOException {
		if (lints.isEmpty()) {
			Files.deleteIfExists(lintFile.toPath());
		} else {
			Lint.toFile(lints, lintFile);
		}
	}

	private void deletePreviousResult(File input) throws IOException {
		delete(getOutputFile(input));
		delete(getLintCheckFile(input));
		delete(getLintApplyFile(input));
	}

	private File getOutputFile(File input) {
		return new File(contentDir(), relativize(input));
	}

	private File getLintCheckFile(File input) {
		return new File(lintCheckDir(), relativize(input));
	}

	private File getLintApplyFile(File input) {
		return new File(lintApplyDir(), relativize(input));
	}

	private void delete(File file) throws IOException {
		if (file.isDirectory()) {
			getFs().delete(d -> d.delete(file));
		} else {
			Files.deleteIfExists(file.toPath());
		}
	}

	private String relativize(File input) {
		File projectDir = getProjectDir().get().getAsFile();
		String outputFileName = FormatExtension.relativize(projectDir, input);
		if (outputFileName != null) {
			return outputFileName;
		}
		throw new IllegalArgumentException(StringPrinter.buildString(printer -> {
			printer.println("Spotless error! All target files must be within the project dir.");
			printer.println("  project dir: " + projectDir.getAbsolutePath());
			printer.println("       target: " + input.getAbsolutePath());
		}));
	}
}
