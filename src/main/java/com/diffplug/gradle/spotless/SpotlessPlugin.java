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

import java.util.Map;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;

import com.diffplug.common.base.Errors;

public class SpotlessPlugin implements Plugin<Project> {
	Project project;
	SpotlessExtension spotlessExtension;

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";

	public void apply(Project project) {
		this.project = project;

		// setup the extension
		spotlessExtension = project.getExtensions().create(EXTENSION, SpotlessExtension.class, project);

		// after the project has been evaluated, configure the check and format tasks per source set
		project.afterEvaluate(unused -> Errors.rethrow().run(this::createTasks));
	}

	/** The extension for this plugin. */
	public SpotlessExtension getExtension() {
		return spotlessExtension;
	}

	void createTasks() throws Exception {
		Task rootCheckTask = project.task(EXTENSION + CHECK);
		Task rootApplyTask = project.task(EXTENSION + APPLY);

		for (Map.Entry<String, FormatExtension> entry : spotlessExtension.formats.entrySet()) {
			rootCheckTask.dependsOn(createCheckTask(entry.getKey(), entry.getValue()));
			rootApplyTask.dependsOn(createApplyTask(entry.getKey(), entry.getValue()));
		}

		// Add our check task as a dependency on the global check task
		// getTasks() returns a "live" collection, so this works even if the
		// task doesn't exist at the time this call is made
		project.getTasks()
				.matching(task -> task.getName().equals(JavaBasePlugin.CHECK_TASK_NAME))
				.all(task -> task.dependsOn(rootCheckTask));
	}

	CheckFormatTask createCheckTask(String name, FormatExtension format) throws Exception {
		CheckFormatTask task = project.getTasks().create(EXTENSION + capitalize(name) + CHECK, CheckFormatTask.class);
		// sets toFormat and steps
		format.setupCheckTask(task);
		return task;
	}

	ApplyFormatTask createApplyTask(String name, FormatExtension format) throws Exception {
		ApplyFormatTask task = project.getTasks().create(EXTENSION + capitalize(name) + APPLY, ApplyFormatTask.class);
		// sets toFormat and steps
		format.setupApplyTask(task);
		return task;
	}

	static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
