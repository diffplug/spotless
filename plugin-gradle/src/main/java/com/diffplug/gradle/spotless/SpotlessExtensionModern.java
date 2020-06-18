/*
 * Copyright 2016-2020 DiffPlug
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
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public class SpotlessExtensionModern extends SpotlessExtensionBase {
	public SpotlessExtensionModern(Project project) {
		super(project);
		rootCheckTask = project.task(EXTENSION + CHECK);
		rootCheckTask.setGroup(TASK_GROUP);
		rootCheckTask.setDescription(CHECK_DESCRIPTION);
		rootApplyTask = project.task(EXTENSION + APPLY);
		rootApplyTask.setGroup(TASK_GROUP);
		rootApplyTask.setDescription(APPLY_DESCRIPTION);
		rootDiagnoseTask = project.task(EXTENSION + DIAGNOSE);
		rootDiagnoseTask.setGroup(TASK_GROUP);	// no description on purpose

		project.afterEvaluate(unused -> {
			if (enforceCheck) {
				project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME)
						.configure(task -> task.dependsOn(rootCheckTask));
			}
		});
	}

	final Task rootCheckTask, rootApplyTask, rootDiagnoseTask;

	@Override
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		// TODO level 1: implement SpotlessExtension::createFormatTasks, but using config avoidance
		// TODO level 2: override configure(String name, Class<T> clazz, Action<T> configure) so that it is lazy

		boolean isIdeHook = project.hasProperty(IdeHook.PROPERTY);
		TaskContainer tasks = project.getTasks();

		// create the SpotlessTask
		String taskName = EXTENSION + SpotlessPlugin.capitalize(name);
		TaskProvider<SpotlessTaskModern> spotlessTask = tasks.register(taskName, SpotlessTaskModern.class, task -> {
			task.setEnabled(!isIdeHook);
			// clean removes the SpotlessCache, so we have to run after clean
			task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
		});

		project.afterEvaluate(unused -> spotlessTask.configure(formatExtension::setupTask));

		// create the check and apply control tasks
		TaskProvider<SpotlessApply> applyTask = tasks.register(taskName + APPLY, SpotlessApply.class, task -> {
			task.setEnabled(!isIdeHook);
			task.dependsOn(spotlessTask);
			task.setSpotlessOutDirectory(spotlessTask.get().getOutputDirectory());
			task.linkSource(spotlessTask.get());
		});
		TaskProvider<SpotlessCheck> checkTask = tasks.register(taskName + CHECK, SpotlessCheck.class, task -> {
			task.setEnabled(!isIdeHook);
			task.dependsOn(spotlessTask);
			task.setSpotlessOutDirectory(spotlessTask.get().getOutputDirectory());
			task.source = spotlessTask.get();

			// if the user runs both, make sure that apply happens first,
			task.mustRunAfter(applyTask);
		});

		// the root tasks depend on the control tasks
		rootCheckTask.dependsOn(checkTask);
		rootApplyTask.dependsOn(applyTask);

		// create the diagnose task
		TaskProvider<SpotlessDiagnoseTask> diagnoseTask = tasks.register(taskName + DIAGNOSE, SpotlessDiagnoseTask.class, task -> {
			task.source = spotlessTask.get();
			task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
		});
		rootDiagnoseTask.dependsOn(diagnoseTask);

		if (isIdeHook) {
			// the rootApplyTask is no longer just a marker task, now it does a bit of work itself
			rootApplyTask.doLast(unused -> IdeHook.performHook(spotlessTask.get()));
		}
	}
}
