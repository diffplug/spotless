/*
 * Copyright 2016 DiffPlug
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import com.diffplug.common.base.Preconditions;
import com.diffplug.common.base.Splitter;
import com.diffplug.common.collect.Iterators;

public class FormatTask extends DefaultTask {
	public Iterable<File> target;
	public boolean check = false;
	public LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX_POLICY;
	public List<FormatterStep> steps = new ArrayList<>();

	@TaskAction
	public void format() throws Exception {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> toFormat'");
		}
		// combine them into the master formatter
		Formatter formatter = new Formatter(lineEndingsPolicy, getProject().getProjectDir().toPath(), steps);

		// perform the check
		if (check) {
			formatCheck(formatter);
		} else {
			formatApply(formatter);
		}
	}

	private static final int MAX_CHECK_MESSAGE_LINES = 50;
	private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n');

	/** Checks the format. */
	private void formatCheck(Formatter formatter) throws IOException {
		List<File> problemFiles = new ArrayList<>();

		for (File file : target) {
			getLogger().debug("Checking format on " + file);
			// keep track of the problem toFormat
			if (!formatter.isClean(file)) {
				problemFiles.add(file);
			}
		}

		if (!problemFiles.isEmpty()) {
			throw formatViolationsFor(formatter, problemFiles);
		}
	}

	/** Returns an exception which indicates problem files nicely. */
	GradleException formatViolationsFor(Formatter formatter, List<File> problemFiles) throws IOException {
		Preconditions.checkArgument(!problemFiles.isEmpty(), "Problem files must not be empty");

		Path rootDir = getProject().getRootDir().toPath();

		StringBuilder messageBuffer = new StringBuilder(64 * MAX_CHECK_MESSAGE_LINES);
		int linesProcessed = 1;
		int numProblemFiles = problemFiles.size();
		int problemFilesRemaining = numProblemFiles;

		for (int i = 0; i < numProblemFiles; i++) {
			File file = problemFiles.get(i);

			String filePath = "  " + rootDir.relativize(file.toPath());
			String diff = prependLinesOf(DiffUtils.diff(file, formatter), "    ");
			String newMessagePart = '\n' + filePath + '\n' + diff;
			if (i < numProblemFiles - 1) {
				newMessagePart += '\n'; // separate violations from one another visually
			}
			int newLinesProcessed = linesProcessed + Iterators.size(NEWLINE_SPLITTER.split(newMessagePart).iterator());
			if (newLinesProcessed > MAX_CHECK_MESSAGE_LINES) {
				// then the message buffer is getting too big
				messageBuffer.append("\n    Problems in ").append(problemFilesRemaining);
				if (i != 0) {
					messageBuffer.append(" additional");
				}
				messageBuffer.append(" file(s)\n");
				break;
			}

			// there's room in the message buffer, so add the format problem to it
			messageBuffer.append(newMessagePart);

			linesProcessed = newLinesProcessed;
			problemFilesRemaining--;
		}

		return new GradleException("The following files had format violations:"
				+ messageBuffer
				+ "\nFormat violations were found. Run 'gradlew " +
				SpotlessPlugin.EXTENSION +
				SpotlessPlugin.APPLY +
				"' to fix them.");
	}

	private String prependLinesOf(String value, String prefix) {
		StringBuilder result = new StringBuilder();
		List<String> lines = NEWLINE_SPLITTER.splitToList(value);
		for (int i = 0; i < lines.size() - 1; i++) {
			result.append(prefix).append(lines.get(i)).append('\n');
		}
		result.append(prefix).append(lines.get(lines.size() - 1));
		return result.toString();
	}

	/** Applies the format. */
	private void formatApply(Formatter formatter) throws IOException {
		for (File file : target) {
			getLogger().debug("Applying format to " + file);
			// keep track of the problem toFormat
			formatter.applyFormat(file);
		}
	}
}
