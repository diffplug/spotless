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

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskProvider;

import com.diffplug.spotless.SpotlessCache;

import static com.diffplug.gradle.spotless.SpotlessTaskConstants.*;

final class SpotlessTaskSetupConfigAvoidance implements SpotlessTaskSetup {
	@Override
	public void accept(Project project, SpotlessExtension spotlessExtension) {
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

		String filePatterns;
		if (project.hasProperty(FILES_PROPERTY) && project.property(FILES_PROPERTY) instanceof String) {
			filePatterns = (String) project.property(FILES_PROPERTY);
		} else {
			// needs to be non-null since it is an @Input property of the task
			filePatterns = "";
		}

		spotlessExtension.formats.forEach((key, value) -> {
			// create the task that does the work
			String taskName = EXTENSION + capitalize(key);
			TaskProvider<SpotlessTask> spotlessTaskProvider = project.getTasks().register(taskName, SpotlessTask.class, task -> {
				value.setupTask(task);
				task.setFilePatterns(filePatterns);
			});

			// create the check and apply control tasks
			TaskProvider<Task> checkTaskProvider = project.getTasks().register(taskName + CHECK);
			TaskProvider<Task> applyTaskProvider = project.getTasks().register(taskName + APPLY);
			// the root tasks depend on them
			rootCheckTaskProvider.configure(rootCheckTask -> rootCheckTask.dependsOn(checkTaskProvider));
			rootApplyTaskProvider.configure(rootApplyTask -> rootApplyTask.dependsOn(applyTaskProvider));
			// and they depend on the work task
			checkTaskProvider.configure(checkTask -> {
				checkTask.dependsOn(spotlessTaskProvider);
			});
			applyTaskProvider.configure(applyTask -> {
				applyTask.dependsOn(spotlessTaskProvider);
			});

			project.getGradle().getTaskGraph().whenReady(graph -> {
				spotlessTaskProvider.configure(spotlessTask -> {
					for(Task t : graph.getAllTasks()) {
						if(t.getName().equals(checkTaskProvider.getName())) {
							spotlessTask.setCheck();
						}
						if(t.getName().equals(applyTaskProvider.getName())) {
							spotlessTask.setApply();
						}
  					}
				});
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
}
