/*
 * Copyright 2015 DiffPlug
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

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;

import java.util.Map;

public class SpotlessPlugin implements Plugin<Project> {
	static final String EXTENSION = "spotless";
	static final String APPLY = "Apply";
	private static final String CHECK = "Check";
	private Project project;
	private SpotlessExtension extension;

	private static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}

	public void apply(Project project) {
		this.project = project;

		// setup the extension
		extension = project.getExtensions().create(EXTENSION, SpotlessExtension.class, project);
		// ExtensionContainer container = ((ExtensionAware) project.getExtensions().getByName(EXTENSION)).getExtensions();

		// after the project has been evaluated, configure the check and format tasks per source set
		project.afterEvaluate(new Action<Project>() {
			@Override
			public void execute(Project project) {
				try {
					createTasks();
				}
				catch (Exception e) {
					throw new GradleException("failed to configure", e);
				}
			}
		});
	}

	/** The extension for this plugin. */
	public SpotlessExtension getExtension() {
		return extension;
	}

	void createTasks() throws Exception {
		Task rootCheckTask = project.task(EXTENSION + CHECK);
		Task rootApplyTask = project.task(EXTENSION + APPLY);

		for (Map.Entry<String, FormatExtension> entry : extension.formats.entrySet()) {
			rootCheckTask.dependsOn(createTask(entry.getKey(), entry.getValue(), true));
			rootApplyTask.dependsOn(createTask(entry.getKey(), entry.getValue(), false));
		}

		// Add our check task as a dependency on the global check task
		// getTasks() returns a "live" collection, so this works even if the
		// task doesn't exist at the time this call is made
		for (Task task : project.getTasks()) {
			if (task.getName().equals(JavaBasePlugin.CHECK_TASK_NAME)) {
				task.dependsOn(rootCheckTask);
			}
		}
	}

	FormatTask createTask(String name, FormatExtension subExtension, boolean check) throws Exception {
		FormatTask task = project.getTasks().create(EXTENSION + capitalize(name) + (check ? CHECK : APPLY), FormatTask.class);
		task.lineEndings = extension.lineEndings;
		task.check = check;
		// sets toFormat and steps
		subExtension.setupTask(task);
		return task;
	}
}
