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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.LineEnding;

public abstract class BaseFormatTask extends DefaultTask {
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

	protected Formatter buildFormatter() {
		if (target == null) {
			throw new GradleException("You must specify 'Iterable<File> toFormat'");
		}
		// combine them into the master formatter
		return Formatter.builder()
				.lineEndingsPolicy(lineEndingsPolicy)
				.encoding(Charset.forName(encoding))
				.rootDir(getProject().getProjectDir().toPath())
				.steps(steps)
				.build();
	}
}
