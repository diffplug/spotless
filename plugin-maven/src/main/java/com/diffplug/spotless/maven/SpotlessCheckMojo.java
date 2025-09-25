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
package com.diffplug.spotless.maven;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.LintState;
import com.diffplug.spotless.extra.integration.DiffMessageFormatter;
import com.diffplug.spotless.maven.incremental.UpToDateChecker;

/**
 * Performs code formatting analysis and prints all violations to the console.
 * Fails the build if violations are discovered.
 */
@Mojo(name = AbstractSpotlessMojo.GOAL_CHECK, defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SpotlessCheckMojo extends AbstractSpotlessMojo {

	private static final String INCREMENTAL_MESSAGE_PREFIX = "Spotless Violation: ";

	public enum MessageSeverity {
		WARNING(BuildContext.SEVERITY_WARNING), ERROR(BuildContext.SEVERITY_ERROR);

		private final int severity;

		MessageSeverity(int severity) {
			this.severity = severity;
		}

		public int getSeverity() {
			return severity;
		}
	}

	/**
	 * The severity used to emit messages during incremental builds.
	 * Either {@code WARNING} or {@code ERROR}.
	 * @see AbstractSpotlessMojo#m2eEnableForIncrementalBuild
	 */
	@Parameter(defaultValue = "WARNING")
	private MessageSeverity m2eIncrementalBuildMessageSeverity;

	@Override
	protected void process(String name, Iterable<File> files, Formatter formatter, UpToDateChecker upToDateChecker) throws MojoExecutionException {
		ImpactedFilesTracker counter = new ImpactedFilesTracker();

		List<File> problemFiles = new ArrayList<>();
		List<Map.Entry<File, LintState>> lintProblems = new ArrayList<>();
		for (File file : files) {
			if (upToDateChecker.isUpToDate(file.toPath())) {
				counter.skippedAsCleanCache();
				if (getLog().isDebugEnabled()) {
					getLog().debug("Spotless will not check an up-to-date file: " + file);
				}
				continue;
			}
			buildContext.removeMessages(file);
			try {
				LintState lintState = super.calculateLintState(formatter, file);
				boolean hasDirtyState = !lintState.getDirtyState().isClean() && !lintState.getDirtyState().didNotConverge();
				boolean hasUnsuppressedLints = lintState.isHasLints();

				if (hasDirtyState || hasUnsuppressedLints) {
					if (hasUnsuppressedLints) {
						lintProblems.add(Map.entry(file, lintState));
					} else {
						problemFiles.add(file);
					}
					if (buildContext.isIncremental()) {
						Map.Entry<Integer, String> diffEntry = DiffMessageFormatter.diff(baseDir.toPath(), formatter, file);
						buildContext.addMessage(file, diffEntry.getKey() + 1, 0, INCREMENTAL_MESSAGE_PREFIX + diffEntry.getValue(), m2eIncrementalBuildMessageSeverity.getSeverity(), null);
					}
					counter.cleaned();
				} else {
					counter.checkedButAlreadyClean();
					upToDateChecker.setUpToDate(file.toPath());
				}
			} catch (IOException | RuntimeException e) {
				throw new MojoExecutionException("Unable to check file " + file, e);
			}
		}

		// We print the number of considered files which is useful when ratchetFrom is setup
		if (counter.getTotal() > 0) {
			getLog().info("Spotless.%s is keeping %s files clean - %s needs changes to be clean, %s were already clean, %s were skipped because caching determined they were already clean".formatted(
					name, counter.getTotal(), counter.getCleaned(), counter.getCheckedButAlreadyClean(), counter.getSkippedAsCleanCache()));
		} else {
			getLog().debug("Spotless.%s has no target files. Examine your `<includes>`: https://github.com/diffplug/spotless/tree/main/plugin-maven#quickstart".formatted(name));
		}

		if (!problemFiles.isEmpty()) {
			// Prioritize formatting violations first (matching Gradle behavior)
			throw new MojoExecutionException(DiffMessageFormatter.builder()
					.runToFix("Run 'mvn spotless:apply' to fix these violations.")
					.formatter(baseDir.toPath(), formatter)
					.problemFiles(problemFiles)
					.getMessage());
		} else if (!lintProblems.isEmpty()) {
			// Show lints only if there are no formatting violations
			Map.Entry<File, LintState> firstLintProblem = lintProblems.getFirst();
			File file = firstLintProblem.getKey();
			LintState lintState = firstLintProblem.getValue();
			String stepName = lintState.getLintsByStep(formatter).keySet().iterator().next();
			throw new MojoExecutionException("Unable to format file %s%nStep '%s' found problem in '%s':%n%s".formatted(
					file, stepName, file.getName(), lintState.asStringOneLine(file, formatter)));
		}
	}
}
