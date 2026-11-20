/*
 * Copyright 2016-2026 DiffPlug
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "not worth caching")
public abstract class SpotlessApply extends SpotlessTaskService.ClientTask {
	/**
	 * Plain-text report of the files which the most recent run of this task rewrote:
	 * one project-relative, {@code /}-separated path per line, sorted, UTF-8.
	 * The report is empty if nothing was rewritten, and it is written even if the task
	 * then fails because of lints.
	 * <p>
	 * Defaults to {@code build/spotless/<taskName>-changed-files.txt}.
	 * <p>
	 * Declared as an output, so a task which uses this provider as an input automatically
	 * depends on this task. Because this task has no inputs, Gradle would otherwise consider
	 * it up-to-date whenever the report is unchanged, so the task is forced to always run.
	 */
	@OutputFile
	public abstract RegularFileProperty getChangedFilesReport();

	@Override
	void init(TaskProvider<SpotlessTaskImpl> impl) {
		super.init(impl);
		getChangedFilesReport().convention(getProject().getLayout().getBuildDirectory()
				.file("spotless/" + getName() + "-changed-files.txt"));
		// apply rewrites sources in place, so it must never be skipped as up-to-date
		getOutputs().upToDateWhen(t -> false);
	}

	@TaskAction
	public void performAction() {
		getTaskService().get().registerApplyAlreadyRan(this);
		ConfigurableFileTree cleanFiles = getConfigCacheWorkaround().fileTree().from(getSpotlessCleanDirectory().get());
		ConfigurableFileTree lintsFiles = getConfigCacheWorkaround().fileTree().from(getSpotlessLintsDirectory().get());
		List<String> changedFiles = new ArrayList<>();
		cleanFiles.visit(new FileVisitor() {
			@Override
			public void visitDir(FileVisitDetails fileVisitDetails) {

			}

			@Override
			public void visitFile(FileVisitDetails fileVisitDetails) {
				String path = fileVisitDetails.getPath();
				File originalSource = new File(getProjectDir().get().getAsFile(), path);
				try {
					if (sameContent(fileVisitDetails.getFile(), originalSource)) {
						// the clean file is stale (see SpotlessCheck#getUncleanFiles), nothing to apply
						return;
					}
					getLogger().debug("Copying " + fileVisitDetails.getFile() + " to " + originalSource);
					Files.copy(fileVisitDetails.getFile().toPath(), originalSource.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
					changedFiles.add(path);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
		Collections.sort(changedFiles);
		writeChangedFilesReport(changedFiles);
		// only report work if a source file was actually rewritten (#2715)
		getState().setDidWork(!changedFiles.isEmpty());
		if (!lintsFiles.isEmpty()) {
			boolean detailed = false;
			throw new GradleException(super.allLintsErrorMsgDetailed(lintsFiles, detailed));
		}
	}

	private void writeChangedFilesReport(List<String> changedFiles) {
		Path report = getChangedFilesReport().get().getAsFile().toPath().toAbsolutePath();
		StringBuilder content = new StringBuilder();
		for (String changedFile : changedFiles) {
			content.append(changedFile).append('\n');
		}
		try {
			Path parent = report.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Files.write(report, content.toString().getBytes(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean sameContent(File clean, File original) throws IOException {
		return original.isFile() && Files.mismatch(clean.toPath(), original.toPath()) == -1L;
	}
}
