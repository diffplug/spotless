/*
 * Copyright 2016-2024 DiffPlug
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
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public class SpotlessExtensionImpl extends SpotlessExtension {
	final TaskProvider<?> rootCheckTask, rootApplyTask, rootDiagnoseTask;

	final static String PROPERTY = "spotlessIdeHook";

	public SpotlessExtensionImpl(Project project) {
		super(project);
		rootCheckTask = project.getTasks().register(EXTENSION + CHECK, task -> {
			task.setGroup(TASK_GROUP);
			task.setDescription(CHECK_DESCRIPTION);
		});
		rootApplyTask = project.getTasks().register(EXTENSION + APPLY, task -> {
			task.setGroup(TASK_GROUP);
			task.setDescription(APPLY_DESCRIPTION);
		});
		rootDiagnoseTask = project.getTasks().register(EXTENSION + DIAGNOSE, task -> {
			task.setGroup(TASK_GROUP); // no description on purpose
		});

		project.afterEvaluate(unused -> {
			if (enforceCheck) {
				project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(task -> task.dependsOn(rootCheckTask));
			}
		});
	}

	@Override
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		TaskContainer tasks = project.getTasks();
		String ideHookPath = (String) project.findProperty(PROPERTY);

		// create the SpotlessTask
		String taskName = EXTENSION + SpotlessPlugin.capitalize(name);
		TaskProvider<SpotlessTaskImpl> spotlessTask = tasks.register(taskName, SpotlessTaskImpl.class, task -> {
			task.init(getRegisterDependenciesTask().getTaskService());
			task.setGroup(TASK_GROUP);
			// clean removes the SpotlessCache, so we have to run after clean
			task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
		});
		project.afterEvaluate(unused -> {
			spotlessTask.configure(task -> {
				// now that the task is being configured, we execute our actions
				for (Action<FormatExtension> lazyAction : formatExtension.lazyActions) {
					lazyAction.execute(formatExtension);
				}
				// and now we'll setup the task
				formatExtension.setupTask(task);
				if (ideHookPath != null) {
					var ideHookFile = project.file(ideHookPath);
					task.setEnabled(task.getTarget().contains(ideHookFile));
					var newTarget = task.getTarget().filter(ideHookFile::equals);
					task.setTarget(newTarget);
				}
			});
		});

		// create the check and apply control tasks
		TaskProvider<SpotlessApply> applyTask = tasks.register(taskName + APPLY, SpotlessApply.class, task -> {
			task.init(spotlessTask.get());
			task.setGroup(TASK_GROUP);
			task.dependsOn(spotlessTask);
			task.setEnabled(spotlessTask.get().getEnabled());
		});
		rootApplyTask.configure(task -> {
			task.dependsOn(applyTask);
		});

		TaskProvider<SpotlessCheck> checkTask = tasks.register(taskName + CHECK, SpotlessCheck.class, task -> {
			SpotlessTaskImpl source = spotlessTask.get();
			task.setGroup(TASK_GROUP);
			task.init(source);
			task.dependsOn(source);
			task.setEnabled(spotlessTask.get().getEnabled());
			// if the user runs both, make sure that apply happens first,
			task.mustRunAfter(applyTask);
		});
		rootCheckTask.configure(task -> task.dependsOn(checkTask));

		// create the diagnose task
		TaskProvider<SpotlessDiagnoseTask> diagnoseTask = tasks.register(taskName + DIAGNOSE, SpotlessDiagnoseTask.class, task -> {
			task.source = spotlessTask.get();
			task.setGroup(TASK_GROUP);
			task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
		});
		rootDiagnoseTask.configure(task -> task.dependsOn(diagnoseTask));
	}
}
