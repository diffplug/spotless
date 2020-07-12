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
package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;

/**
 * Performs code formatting analysis and prints all violations to the console.
 * Fails the build if violations are discovered.
 */
@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SpotlessCheckMojo extends AbstractSpotlessMojo {

	@Parameter(property = "spotless.check.skip", defaultValue = "false")
	private boolean skip;

	@Override
	protected void process(Iterable<File> files, Formatter formatter) throws MojoExecutionException {
		if (skip) {
			getLog().info("Spotless check skipped");
			return;
		}

		List<File> problemFiles = new ArrayList<>();
		for (File file : files) {
			try {
				PaddedCell.DirtyState dirtyState = PaddedCell.calculateDirtyState(formatter, file);
				if (!dirtyState.isClean() && !dirtyState.didNotConverge()) {
					problemFiles.add(file);
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to format file " + file, e);
			}
		}

		if (!problemFiles.isEmpty()) {
			throw new MojoExecutionException(DiffMessageFormatter.builder()
					.runToFix("Run 'mvn spotless:apply' to fix these violations.")
					.formatter(formatter)
					.problemFiles(problemFiles)
					.getMessage());
		}
	}
}
