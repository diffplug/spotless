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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;
import com.diffplug.spotless.PaddedCellBulk;

public class SpotlessTask extends DefaultTask {
	// set by SpotlessExtension, but possibly overridden by FormatExtension
	@Input
	protected String encoding = "UTF-8";

	public String getEncoding() {
		return encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	@Input
	protected LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX.createPolicy();

	public LineEnding.Policy getLineEndingsPolicy() {
		return lineEndingsPolicy;
	}

	public void setLineEndingsPolicy(LineEnding.Policy lineEndingsPolicy) {
		this.lineEndingsPolicy = lineEndingsPolicy;
	}

	// set by FormatExtension
	@Input
	protected boolean paddedCell = false;

	public boolean isPaddedCell() {
		return paddedCell;
	}

	public void setPaddedCell(boolean paddedCell) {
		this.paddedCell = paddedCell;
	}

	@InputFiles
	@SkipWhenEmpty
	protected Iterable<File> target;

	public Iterable<File> getTarget() {
		return target;
	}

	public void setTarget(Iterable<File> target) {
		this.target = target;
	}

	@Input
	protected List<FormatterStep> steps = new ArrayList<>();

	public List<FormatterStep> getSteps() {
		return Collections.unmodifiableList(steps);
	}

	public void setSteps(List<FormatterStep> steps) {
		this.steps = steps;
	}

	public boolean addStep(FormatterStep step) {
		return this.steps.add(step);
	}

	private boolean check = false;
	private boolean apply = false;

	public void setCheck() {
		this.check = true;
	}

	public void setApply() {
		this.apply = true;
	}

	/** Returns the name of this format. */
	String formatName() {
		String name = getName();
		if (name.startsWith(SpotlessPlugin.EXTENSION)) {
			return name.substring(SpotlessPlugin.EXTENSION.length()).toLowerCase(Locale.ROOT);
		} else {
			return name;
		}
	}

	@TaskAction
	public void performAction(IncrementalTaskInputs inputs) throws Exception {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> toFormat'");
		}
		if (!check && !apply) {
			throw new GradleException("Don't call " + getName() + " directly, call " + getName() + SpotlessPlugin.CHECK + " or " + getName() + SpotlessPlugin.APPLY);
		}

		// create the formatter
		Formatter formatter = Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(Charset.forName(encoding))
				.rootDir(getProject().getProjectDir().toPath())
				.steps(steps)
				.build();
		// find the outOfDate files
		List<File> outOfDate = new ArrayList<>();
		inputs.outOfDate(inputDetails -> outOfDate.add(inputDetails.getFile()));

		if (apply) {
			apply(formatter, outOfDate);
		}
		if (check) {
			check(formatter, outOfDate);
		}
	}

	private void apply(Formatter formatter, List<File> outOfDate) throws Exception {
		if (isPaddedCell()) {
			for (File file : outOfDate) {
				getLogger().debug("Applying format to " + file);
				PaddedCellBulk.apply(formatter, file);
			}
		} else {
			boolean anyMisbehave = false;
			for (File file : outOfDate) {
				getLogger().debug("Applying format to " + file);
				String unixResultIfDirty = formatter.applyToAndReturnResultIfDirty(file);
				// because apply will count as up-to-date, it's important
				// that every call to apply will get a PaddedCell check
				if (!anyMisbehave && unixResultIfDirty != null) {
					String onceMore = formatter.compute(unixResultIfDirty, file);
					//  f(f(input) == f(input) for an idempotent function
					if (!onceMore.equals(unixResultIfDirty)) {
						anyMisbehave = true;
					}
				}
			}
			if (anyMisbehave) {
				throw PaddedCellGradle.youShouldTurnOnPaddedCell(this);
			}
		}
	}

	private void check(Formatter formatter, List<File> outOfDate) throws Exception {
		List<File> problemFiles = new ArrayList<>();
		for (File file : outOfDate) {
			getLogger().debug("Checking format on " + file);
			if (!formatter.isClean(file)) {
				problemFiles.add(file);
			}
		}
		if (paddedCell) {
			PaddedCellGradle.check(this, formatter, problemFiles);
		} else {
			if (!problemFiles.isEmpty()) {
				// if we're not in paddedCell mode, we'll check if maybe we should be
				if (PaddedCellBulk.anyMisbehave(formatter, problemFiles)) {
					throw PaddedCellGradle.youShouldTurnOnPaddedCell(this);
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
}
