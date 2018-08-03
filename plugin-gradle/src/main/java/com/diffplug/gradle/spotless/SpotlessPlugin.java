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
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskProvider;

import com.diffplug.spotless.SpotlessCache;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import groovy.lang.Closure;

public class SpotlessPlugin implements Plugin<Project> {
	SpotlessExtension spotlessExtension;

	static final String EXTENSION = "spotless";
	static final String CHECK = "Check";
	static final String APPLY = "Apply";

	private static final String TASK_GROUP = "Verification";
	private static final String CHECK_DESCRIPTION = "Checks that sourcecode satisfies formatting steps.";
	private static final String APPLY_DESCRIPTION = "Applies code formatting steps to sourcecode in-place.";

	@Override
	public void apply(Project project) {
		// make sure there's a `clean` task
		project.getPlugins().apply(BasePlugin.class);

		// setup the extension
		spotlessExtension = project.getExtensions().create(EXTENSION, SpotlessExtension.class, project);

		// after the project has been evaluated, configure the check and format tasks per source set
		project.afterEvaluate(this::createTasks);
	}

	/** The extension for this plugin. */
	public SpotlessExtension getExtension() {
		return spotlessExtension;
	}

	@SuppressWarnings("rawtypes")
	void createTasks(Project project) {
		TaskProvider<Task> rootCheckTaskProvider = project.getTasks().register(
				EXTENSION + CHECK,
				task -> {
					task.setGroup(TASK_GROUP);
					task.setDescription(CHECK_DESCRIPTION);
				});
		TaskProvider<Task> rootApplyTaskProvider = project.getTasks().register(
				EXTENSION + APPLY,
				task -> {
					task.setGroup(TASK_GROUP);
					task.setDescription(APPLY_DESCRIPTION);
				});

		spotlessExtension.formats.forEach((key, value) -> {
			// create the task that does the work
			String taskName = EXTENSION + capitalize(key);
			TaskProvider<SpotlessTask> spotlessTaskProvider = project.getTasks().register(taskName, SpotlessTask.class);
			spotlessTaskProvider.configure(value::setupTask);

			// create the check and apply control tasks
			TaskProvider<Task> checkTaskProvider = project.getTasks().register(taskName + CHECK);
			TaskProvider<Task> applyTaskProvider = project.getTasks().register(taskName + APPLY);
			// the root tasks depend on them
			rootCheckTaskProvider.configure(rootCheckTask -> rootCheckTask.dependsOn(checkTaskProvider));
			rootApplyTaskProvider.configure(rootApplyTask -> rootApplyTask.dependsOn(applyTaskProvider));
			// and they depend on the work task
			checkTaskProvider.configure(checkTask -> checkTask.dependsOn(spotlessTaskProvider));
			applyTaskProvider.configure(applyTask -> applyTask.dependsOn(spotlessTaskProvider));

			// when the task graph is ready, we'll configure the spotlessTask appropriately
			// TODO: Consider swapping out the Closure below for a type-safe Action<TaskExecutionGraph>
			project.getGradle().getTaskGraph().whenReady(new Closure(null) {
				private static final long serialVersionUID = 1L;

				// called by gradle
				@SuppressFBWarnings("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS")
				public Object doCall(TaskExecutionGraph graph) {
					if (checkTaskProvider.isPresent() && graph.hasTask(checkTaskProvider.get())) {
						spotlessTaskProvider.configure(SpotlessTask::setCheck);
					}
					if (applyTaskProvider.isPresent() && graph.hasTask(applyTaskProvider.get())) {
						spotlessTaskProvider.configure(SpotlessTask::setApply);
					}
					return Closure.DONE;
				}
			});
		});

		// Add our check task as a dependency on the global check task.
		// getTasks() returns a "live" collection and configureEach() is lazy,
		// so this works even if the task doesn't exist at the time this call
		// is made.
		if (spotlessExtension.enforceCheck) {
			project.getTasks()
					.matching(task -> task.getName().equals(JavaBasePlugin.CHECK_TASK_NAME))
					.configureEach(task -> task.dependsOn(rootCheckTaskProvider));
		}

		// clear spotless' cache when the user does a clean, but only after any spotless tasks
		TaskProvider<Task> cleanTaskProvider = project.getTasks().named(BasePlugin.CLEAN_TASK_NAME);
		cleanTaskProvider.configure(task -> task.doLast(unused -> SpotlessCache.clear()));
		project.getTasks()
				.withType(SpotlessTask.class)
				.configureEach(task -> task.mustRunAfter(cleanTaskProvider));
	}

	static String capitalize(String input) {
		return Character.toUpperCase(input.charAt(0)) + input.substring(1);
	}
}
