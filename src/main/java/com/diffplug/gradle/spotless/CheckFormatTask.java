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
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.common.base.Errors;

public class CheckFormatTask extends BaseFormatTask {
	@TaskAction
	public void check(IncrementalTaskInputs inputs) throws Exception {
		Formatter formatter = buildFormatter();
		List<File> problemFiles = new ArrayList<>();

		inputs.outOfDate(inputDetails -> {
			try {
				if (formatter.isClean(inputDetails.getFile())) {
					problemFiles.add(inputDetails.getFile());
				}
			} catch (IOException e) {
				throw Errors.asRuntime(e);
			}
		});

		for (File file : target) {
			getLogger().debug("Checking format on " + file);
			// keep track of the problem toFormat
			try {
				if (!formatter.isClean(file)) {
					problemFiles.add(file);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
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

	/** Returns the name of this format. */
	String getFormatName() {
		String name = getName();
		if (name.startsWith(SpotlessPlugin.EXTENSION)) {
			String after = name.substring(SpotlessPlugin.EXTENSION.length());
			return after.substring(0, after.length() - SpotlessPlugin.CHECK.length()).toLowerCase(Locale.US);
		}
		return name;
	}
}
