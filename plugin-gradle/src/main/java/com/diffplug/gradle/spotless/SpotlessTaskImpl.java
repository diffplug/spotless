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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
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

import com.diffplug.common.annotations.VisibleForTesting;
import com.diffplug.common.base.StringPrinter;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.Lint;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.extra.GitRatchet;

@CacheableTask
public abstract class SpotlessTaskImpl extends SpotlessTask {
	@Internal
	abstract DirectoryProperty getProjectDir();

	void init(Provider<SpotlessTaskService> service) {
		taskServiceProvider = service;
		SpotlessTaskService.usesServiceTolerateTestFailure(this, service);
		getTaskService().set(service);
		getProjectDir().set(getProject().getProjectDir());
	}

	// this field is stupid, but we need it, see https://github.com/diffplug/spotless/issues/1260
	private transient Provider<SpotlessTaskService> taskServiceProvider;

	@Internal
	Provider<SpotlessTaskService> getTaskServiceProvider() {
		return taskServiceProvider;
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
			getFs().delete(d -> d.delete(cleanDirectory));
			getFs().delete(d -> d.delete(lintsDirectory));
			Files.createDirectories(cleanDirectory.toPath());
			Files.createDirectories(lintsDirectory.toPath());
		}

		try (Formatter formatter = buildFormatter()) {
			GitRatchetGradle ratchet = getRatchet();
			for (FileChange fileChange : inputs.getFileChanges(target)) {
				File input = fileChange.getFile();
				File projectDir = getProjectDir().get().getAsFile();
				String relativePath = FormatExtension.relativize(projectDir, input);
				if (relativePath == null) {
					throw new IllegalArgumentException(StringPrinter.buildString(printer -> {
						printer.println("Spotless error! All target files must be within the project dir.");
						printer.println("  project dir: " + projectDir.getAbsolutePath());
						printer.println("       target: " + input.getAbsolutePath());
					}));
				}
				if (fileChange.getChangeType() == ChangeType.REMOVED) {
					deletePreviousResults(cleanDirectory, relativePath);
					deletePreviousResults(lintsDirectory, relativePath);
				} else {
					if (input.isFile()) {
						processInputFile(ratchet, formatter, input, relativePath);
					}
				}
			}
		}
	}

	@VisibleForTesting
	void processInputFile(@Nullable GitRatchet ratchet, Formatter formatter, File input, String relativePath) throws IOException {
		File cleanFile = new File(cleanDirectory, relativePath);
		File lintFile = new File(lintsDirectory, relativePath);
		getLogger().debug("Applying format to {} and writing to {}", input, cleanFile);
		LintState lintState;
		if (ratchet != null && ratchet.isClean(getProjectDir().get().getAsFile(), getRootTreeSha(), input)) {
			lintState = LintState.clean();
		} else {
			try {
				lintState = LintState.of(formatter, input).withRemovedSuppressions(formatter, relativePath, getLintSuppressions());
			} catch (Throwable e) {
				throw new IllegalArgumentException("Issue processing file: " + input, e);
			}
		}
		if (lintState.getDirtyState().isClean()) {
			// Remove previous output if it exists
			Files.deleteIfExists(cleanFile.toPath());
		} else if (lintState.getDirtyState().didNotConverge()) {
			getLogger().warn("Skipping '{}' because it does not converge.  Run {@code spotlessDiagnose} to understand why", relativePath);
		} else {
			Path parentDir = cleanFile.toPath().getParent();
			if (parentDir == null) {
				throw new IllegalStateException("Every file has a parent folder. But not: " + cleanFile);
			}
			Files.createDirectories(parentDir);
			// Need to copy the original file to the tmp location just to remember the file attributes
			Files.copy(input.toPath(), cleanFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);

			getLogger().info(String.format("Writing clean file: %s", cleanFile));
			lintState.getDirtyState().writeCanonicalTo(cleanFile);
		}
		if (!lintState.isHasLints()) {
			Files.deleteIfExists(lintFile.toPath());
		} else {
			LinkedHashMap<String, List<Lint>> lints = lintState.getLintsByStep(formatter);
			SerializableMisc.toFile(lints, lintFile);
		}
	}

	private void deletePreviousResults(File baseDir, String subpath) throws IOException {
		File output = new File(baseDir, subpath);
		if (output.isDirectory()) {
			getFs().delete(d -> d.delete(output));
		} else {
			Files.deleteIfExists(output.toPath());
		}
	}
}
