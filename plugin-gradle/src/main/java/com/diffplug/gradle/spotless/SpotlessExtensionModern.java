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

		// create the SpotlessTask
		String taskName = EXTENSION + SpotlessPlugin.capitalize(name);
		SpotlessTaskModern spotlessTask = project.getTasks().create(taskName, SpotlessTaskModern.class);
		project.afterEvaluate(unused -> formatExtension.setupTask(spotlessTask));

		// clean removes the SpotlessCache, so we have to run after clean
		Task clean = project.getTasks().getByName(BasePlugin.CLEAN_TASK_NAME);
		spotlessTask.mustRunAfter(clean);

		// create the check and apply control tasks
		SpotlessCheck checkTask = project.getTasks().create(taskName + CHECK, SpotlessCheck.class);
		checkTask.setSpotlessOutDirectory(spotlessTask.getOutputDirectory());
		checkTask.source = spotlessTask;
		checkTask.dependsOn(spotlessTask);

		SpotlessApply applyTask = project.getTasks().create(taskName + APPLY, SpotlessApply.class);
		applyTask.setSpotlessOutDirectory(spotlessTask.getOutputDirectory());
		applyTask.linkSource(spotlessTask);
		applyTask.dependsOn(spotlessTask);

		// if the user runs both, make sure that apply happens first,
		checkTask.mustRunAfter(applyTask);

		// the root tasks depend on the control tasks
		rootCheckTask.dependsOn(checkTask);
		rootApplyTask.dependsOn(applyTask);

		// create the diagnose task
		SpotlessDiagnoseTask diagnoseTask = project.getTasks().create(taskName + DIAGNOSE, SpotlessDiagnoseTask.class);
		diagnoseTask.source = spotlessTask;
		rootDiagnoseTask.dependsOn(diagnoseTask);
		diagnoseTask.mustRunAfter(clean);

		if (project.hasProperty(IdeHook.PROPERTY)) {
			// disable the normal tasks, to disable their up-to-date checking
			spotlessTask.setEnabled(false);
			checkTask.setEnabled(false);
			applyTask.setEnabled(false);
			// the rootApplyTask is no longer just a marker task, now it does a bit of work itself
			rootApplyTask.doLast(unused -> IdeHook.performHook(spotlessTask));
		}
	}
}
