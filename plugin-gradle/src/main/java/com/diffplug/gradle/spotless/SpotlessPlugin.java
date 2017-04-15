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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionGraph;
import org.gradle.api.plugins.JavaBasePlugin;

import com.diffplug.common.base.Errors;
import com.diffplug.spotless.SpotlessCache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;

public class SpotlessPlugin implements Plugin<Project> {
	Project project;
	SpotlessExtension spotlessExtension;

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";

	private static final String TASK_GROUP = "Verification";
	private static final String CHECK_DESCRIPTION = "Checks that sourcecode satisfies formatting steps.";
	private static final String APPLY_DESCRIPTION = "Applies code formatting steps to sourcecode in-place.";

	@Override
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

	@SuppressWarnings("rawtypes")
	void createTasks() throws Exception {
		Task rootCheckTask = project.task(EXTENSION + CHECK);
		rootCheckTask.setGroup(TASK_GROUP);
		rootCheckTask.setDescription(CHECK_DESCRIPTION);
		Task rootApplyTask = project.task(EXTENSION + APPLY);
		rootApplyTask.setGroup(TASK_GROUP);
		rootApplyTask.setDescription(APPLY_DESCRIPTION);

		spotlessExtension.formats.forEach((key, value) -> {
			// create the task that does the work
			String taskName = EXTENSION + capitalize(key);
			SpotlessTask spotlessTask = project.getTasks().create(taskName, SpotlessTask.class);
			value.setupTask(spotlessTask);

			// create the check and apply control tasks
			Task checkTask = project.getTasks().create(taskName + CHECK);
			Task applyTask = project.getTasks().create(taskName + APPLY);
			// the root tasks depend on them
			rootCheckTask.dependsOn(checkTask);
			rootApplyTask.dependsOn(applyTask);
			// and they depend on the work task
			checkTask.dependsOn(spotlessTask);
			applyTask.dependsOn(spotlessTask);

			// when the task graph is ready, we'll configure the spotlessTask appropriately
			project.getGradle().getTaskGraph().whenReady(new Closure(null) {
				private static final long serialVersionUID = 1L;

				// called by gradle
				@SuppressFBWarnings("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS")
				public Object doCall(TaskExecutionGraph graph) {
					if (graph.hasTask(checkTask)) {
						spotlessTask.setCheck();
					}
					if (graph.hasTask(applyTask)) {
						spotlessTask.setApply();
					}
					return Closure.DONE;
				}
			});
		});

		// Add our check task as a dependency on the global check task
		// getTasks() returns a "live" collection, so this works even if the
		// task doesn't exist at the time this call is made
		if (spotlessExtension.enforceCheck) {
			project.getTasks()
					.matching(task -> task.getName().equals(JavaBasePlugin.CHECK_TASK_NAME))
					.all(task -> task.dependsOn(rootCheckTask));
		}

		// clear spotless' cache when the user does a clean
		project.getTasks()
				.matching(task -> task.getName().equals("clean"))
				.all(task -> task.doLast(unused -> SpotlessCache.clear()));
	}

	static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
