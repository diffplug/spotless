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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

public class FormatTask extends DefaultTask {
	// set by SpotlessExtension
	public Charset encoding = StandardCharsets.UTF_8;
	public LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX_POLICY;
	// set by FormatExtension
	public boolean paddedCell = false;
	public Iterable<File> target;
	public List<FormatterStep> steps = new ArrayList<>();
	// set by plugin
	public boolean check = false;

	/** Returns the name of this format. */
	public String getFormatName() {
		String name = getName();
		if (name.startsWith(SpotlessPlugin.EXTENSION)) {
			String after = name.substring(SpotlessPlugin.EXTENSION.length());
			if (after.endsWith(SpotlessPlugin.CHECK)) {
				return after.substring(0, after.length() - SpotlessPlugin.CHECK.length()).toLowerCase(Locale.US);
			} else if (after.endsWith(SpotlessPlugin.APPLY)) {
				return after.substring(0, after.length() - SpotlessPlugin.APPLY.length()).toLowerCase(Locale.US);
			}
		}
		return name;
	}

	@TaskAction
	public void format() throws Exception {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> toFormat'");
		}
		// combine them into the master formatter
		Formatter formatter = new Formatter(lineEndingsPolicy, encoding, getProject().getProjectDir().toPath(), steps);

		// perform the check
		if (check) {
			formatCheck(formatter);
		} else {
			formatApply(formatter);
		}
	}

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

		if (paddedCell) {
			PaddedCellTaskMisc.check(this, formatter, problemFiles);
		} else {
			if (!problemFiles.isEmpty()) {
				// if we're not in paddedCell mode, we'll check if maybe we should be
				if (PaddedCellTaskMisc.anyMisbehave(formatter, problemFiles)) {
					throw PaddedCellTaskMisc.youShouldTurnOnPaddedCell(this);
				} else {
					throw formatViolationsFor(formatter, problemFiles);
				}
			}
		}
	}

	/** Returns an exception which indicates problem files nicely. */
	GradleException formatViolationsFor(Formatter formatter, List<File> problemFiles) throws IOException {
		return new GradleException(DiffMessageFormatter.messageFor(this, formatter, problemFiles));
	}

	/** Applies the format. */
	private void formatApply(Formatter formatter) throws IOException {
		for (File file : target) {
			getLogger().debug("Applying format to " + file);
			// keep track of the problem toFormat
			if (paddedCell) {
				PaddedCellTaskMisc.apply(this, formatter, file);
			} else {
				formatter.applyFormat(file);
			}
		}
	}
}
