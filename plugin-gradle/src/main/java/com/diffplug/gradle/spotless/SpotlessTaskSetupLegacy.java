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

import static com.diffplug.gradle.spotless.Constants.APPLY;
import static com.diffplug.gradle.spotless.Constants.APPLY_DESCRIPTION;
import static com.diffplug.gradle.spotless.Constants.CHECK;
import static com.diffplug.gradle.spotless.Constants.CHECK_DESCRIPTION;
import static com.diffplug.gradle.spotless.Constants.EXTENSION;
import static com.diffplug.gradle.spotless.Constants.TASK_GROUP;
import static com.diffplug.gradle.spotless.MoreStrings.capitalize;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;

import com.diffplug.spotless.SpotlessCache;

final class SpotlessTaskSetupLegacy implements SpotlessTaskSetup {
	@Override
	public void accept(Project project, SpotlessExtension spotlessExtension) {
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
			project.getGradle().getTaskGraph().whenReady(graph -> {
				if (graph.hasTask(checkTask)) {
					spotlessTask.setCheck();
				}
				if (graph.hasTask(applyTask)) {
					spotlessTask.setApply();
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

		// clear spotless' cache when the user does a clean, but only after any spotless tasks
		Task clean = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
		clean.doLast(unused -> SpotlessCache.clear());
		project.getTasks()
				.withType(SpotlessTask.class)
				.all(task -> task.mustRunAfter(clean));
	}
}
