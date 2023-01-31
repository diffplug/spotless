/*
 * Copyright 2016-2023 DiffPlug
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

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;

/**
 * Performs formatting of all source files according to configured formatters.
 */
@Mojo(name = AbstractSpotlessMojo.GOAL_APPLY, threadSafe = true)
public class SpotlessApplyMojo extends AbstractSpotlessMojo {

	@Override
	protected void process(Iterable<File> files, Formatter formatter, UpToDateChecker upToDateChecker) throws MojoExecutionException {
		ImpactedFilesTracker impactedFilesTracker = new ImpactedFilesTracker();

		for (File file : files) {
			if (upToDateChecker.isUpToDate(file.toPath())) {
				impactedFilesTracker.skippedAsCleanCache();
				if (getLog().isDebugEnabled()) {
					getLog().debug("Spotless will not format an up-to-date file: " + file);
				}
				continue;
			}

			try {
				PaddedCell.DirtyState dirtyState = PaddedCell.calculateDirtyState(formatter, file);
				if (!dirtyState.isClean() && !dirtyState.didNotConverge()) {
					getLog().info(String.format("Writing clean file: %s", file));
					dirtyState.writeCanonicalTo(file);
					buildContext.refresh(file);
					impactedFilesTracker.cleaned();
				} else {
					impactedFilesTracker.checkedButAlreadyClean();
				}
			} catch (IOException e) {
				throw new MojoExecutionException("Unable to format file " + file, e);
			}

			upToDateChecker.setUpToDate(file.toPath());
		}

		// We print the number of considered files which is useful when ratchetFrom is setup
		int skippedAsCleanCache = impactedFilesTracker.getSkippedAsCleanCache();
		int checkedButAlreadyClean = impactedFilesTracker.getCheckedButAlreadyClean();
		int cleaned = impactedFilesTracker.getCleaned();
		int totalProcessed = skippedAsCleanCache + checkedButAlreadyClean + cleaned;
		if (totalProcessed > 0) {
			getLog().info(String.format("Spotless.%s is keeping %s files clean - %s were changed to be clean, %s were already clean, %s were skipped because caching determined they were already clean",
					formatter.getName(), totalProcessed, cleaned, checkedButAlreadyClean, skippedAsCleanCache));
		} else {
			getLog().warn(String.format("Spotless.%s has no target files. Examine your `<includes>`: https://github.com/diffplug/spotless/tree/main/plugin-maven#quickstart", formatter.getName()));
		}
	}
}
