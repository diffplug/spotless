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

import java.util.Objects;

import com.diffplug.spotless.extra.groovy.GrEclipseFormatterStep;
import com.diffplug.spotless.java.ImportOrderStep;

public class GroovyGradleExtension extends FormatExtension {
	private static final String GRADLE_FILE_EXTENSION = "*.gradle";
	static final String NAME = "groovyGradle";

	public GroovyGradleExtension(SpotlessExtension rootExtension) {
		super(rootExtension);
	}

	public void importOrder(String... importOrder) {
		addStep(ImportOrderStep.createFromOrder(importOrder));
	}

	public void importOrderFile(Object importOrderFile) {
		Objects.requireNonNull(importOrderFile);
		addStep(ImportOrderStep.createFromFile(getProject().file(importOrderFile)));
	}

	public GroovyExtension.GrEclipseConfig greclipse() {
		return new GroovyExtension.GrEclipseConfig(GrEclipseFormatterStep.defaultVersion(), this);
	}

	public GroovyExtension.GrEclipseConfig greclipse(String version) {
		return new GroovyExtension.GrEclipseConfig(version, this);
	}

	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = parseTarget(GRADLE_FILE_EXTENSION);
		}
		super.setupTask(task);
	}
}
