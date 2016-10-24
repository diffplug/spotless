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

import java.util.LinkedHashMap;
import java.util.Map;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.diffplug.gradle.spotless.freshmark.FreshMarkExtension;
import com.diffplug.gradle.spotless.java.JavaExtension;

public class SpotlessExtension {
	final Project project;

	public SpotlessExtension(Project project) {
		this.project = project;
	}

	/** Line endings (if any). */
	LineEnding lineEndings = LineEnding.GIT_ATTRIBUTES;

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = lineEndings;
	}

	Map<String, FormatExtension> formats = new LinkedHashMap<>();

	/** Configures the special java-specific extension. */
	public void java(Action<JavaExtension> closure) {
		JavaExtension java = new JavaExtension(this);
		closure.execute(java);
	}

	/** Configures the special freshmark-specific extension. */
	public void freshmark(Action<FreshMarkExtension> closure) {
		FreshMarkExtension freshmark = new FreshMarkExtension(this);
		closure.execute(freshmark);
	}

	/** Configures a custom extension. */
	public void format(String name, Action<FormatExtension> closure) {
		FormatExtension extension = new FormatExtension(name, this);
		closure.execute(extension);
	}

	/** Called by the FormatExtension constructor. */
	void addFormatExtension(FormatExtension extension) {
		FormatExtension former = formats.put(extension.name, extension);
		if (former != null) {
			throw new GradleException("Multiple spotless extensions with name '" + extension.name + "'");
		}
	}

	LineEnding.Policy getLineEndingPolicy() {
		return lineEndings.createPolicy(project.getProjectDir());
	}

	boolean paddedCell = false;

	/** Enables paddedCell mode {@see https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md}. */
	public void paddedCell() {
		paddedCell(true);
	}

	/** Enables or disables paddedCell mode {@see https://github.com/diffplug/spotless/blob/master/PADDEDCELL.md}. */
	public void paddedCell(boolean paddedCell) {
		this.paddedCell = paddedCell;
	}
}
