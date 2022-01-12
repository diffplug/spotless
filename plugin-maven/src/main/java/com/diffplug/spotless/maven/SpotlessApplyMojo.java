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
package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

import com.diffplug.spotless.DirtyState;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;

/**
 * Performs formatting of all source files according to configured formatters.
 */
@Mojo(name = AbstractSpotlessMojo.GOAL_APPLY, threadSafe = true)
public class SpotlessApplyMojo extends AbstractSpotlessMojo {

	@Override
	protected void process(Iterable<File> files, Formatter formatter, UpToDateChecker upToDateChecker) throws MojoExecutionException {
		for (File file : files) {
			if (upToDateChecker.isUpToDate(file.toPath())) {
				if (getLog().isDebugEnabled()) {
					getLog().debug("Spotless will not format an up-to-date file: " + file);
				}
				continue;
			}

			try {
				DirtyState dirtyState = DirtyState.of(formatter, file).calculateDirtyState();
				if (!dirtyState.isClean() && !dirtyState.didNotConverge()) {
					dirtyState.writeCanonicalTo(file);
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to format file " + file, e);
			}

			upToDateChecker.setUpToDate(file.toPath());
		}
	}
}
