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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@UntrackedTask(because = "undeclared inputs/outputs")
public abstract class SpotlessDiagnoseTask extends DefaultTask {

	@Inject
	protected abstract ProjectLayout getProjectLayout();

	@Inject
	protected abstract FileSystemOperations getFileSystemOperations();

	@Input
	protected abstract Property<String> getFormatName();

	@Input
	protected abstract Property<FormatterSpec> getFormatter();

	@InputFiles
	@PathSensitive(PathSensitivity.RELATIVE)
	protected abstract ConfigurableFileCollection getTarget();

	void init(SpotlessTask source) {
		getFormatName().convention(source.formatName());
		getFormatter().convention(source.getFormatterSpec());
		getTarget().setFrom(source.getTarget());
	}

	@TaskAction
	@SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
	public void performAction() throws IOException {
		Path srcRoot = getProjectLayout().getProjectDirectory().getAsFile().toPath();
		Path diagnoseRoot = getProjectLayout().getBuildDirectory().getAsFile().get()
				.toPath().resolve("spotless-diagnose-" + getFormatName().get());
		getFileSystemOperations().delete(spec -> spec.delete(diagnoseRoot));
		try (Formatter formatter = getFormatter().get().buildFormatter()) {
			for (File file : getTarget()) {
				getLogger().debug("Running padded cell check on " + file);
				PaddedCell padded = PaddedCell.check(formatter, file);
				if (!padded.misbehaved()) {
					getLogger().debug("    well-behaved.");
				} else {
					// the file is misbehaved, so we'll write all its steps to DIAGNOSE_DIR
					Path relative = srcRoot.relativize(file.toPath());
					Path diagnoseFile = diagnoseRoot.resolve(relative);
					for (int i = 0; i < padded.steps().size(); ++i) {
						Path path = Paths.get(diagnoseFile + "." + padded.type().name().toLowerCase(Locale.ROOT) + i);
						Files.createDirectories(path.getParent());
						String version = padded.steps().get(i);
						Files.write(path, version.getBytes(formatter.getEncoding()));
					}
					// dump the type of the misbehavior to console
					getLogger().lifecycle("    " + relative + " " + padded.userMessage());
				}
			}
		}
		if (Files.exists(diagnoseRoot)) {
			getLogger().lifecycle("Some formatters are misbehaving, you can see details at " + diagnoseRoot);
		} else {
			getLogger().lifecycle("All formatters are well behaved for all files.");
		}
	}
}
