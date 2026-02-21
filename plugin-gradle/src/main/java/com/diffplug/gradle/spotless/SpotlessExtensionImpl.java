/*
 * Copyright 2016-2026 DiffPlug
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
	final TaskProvider<?> rootCheckTask;
	final TaskProvider<?> rootApplyTask;
	final TaskProvider<?> rootDiagnoseTask;
	final TaskProvider<?> rootInstallPreHook;

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
		rootDiagnoseTask = project.getTasks().register(EXTENSION + DIAGNOSE, task -> task.setGroup(TASK_GROUP));
		rootInstallPreHook = project.getTasks().register(EXTENSION + INSTALL_GIT_PRE_PUSH_HOOK, SpotlessInstallPrePushHookTask.class, task -> {
			task.setGroup(BUILD_SETUP_TASK_GROUP);
			task.setDescription(INSTALL_GIT_PRE_PUSH_HOOK_DESCRIPTION);
			task.getRootDir().set(project.getRootDir());
			task.getIsRootExecution().set(project.equals(project.getRootProject()));
		});

		project.afterEvaluate(unused -> {
			if (enforceCheck) {
				project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME).configure(task -> task.dependsOn(rootCheckTask));
			}
		});
	}

	@Override
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		IdeHook.State ideHook = new IdeHook.State(project);
		TaskContainer tasks = project.getTasks();

		// create the SpotlessTask
		String taskName = EXTENSION + SpotlessPlugin.capitalize(name);
		TaskProvider<SpotlessTaskImpl> spotlessTask = tasks.register(taskName, SpotlessTaskImpl.class, task -> {
			task.init(getSpotlessTaskService());
			task.setGroup(TASK_GROUP);
			task.getIdeHookState().set(ideHook);
			// clean removes the SpotlessCache, so we have to run after clean
			task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
		});
		project.afterEvaluate(unused -> spotlessTask.configure(task -> {
			// now that the task is being configured, we execute our actions
			for (Action<FormatExtension> lazyAction : formatExtension.lazyActions) {
				lazyAction.execute(formatExtension);
			}
			// and now we'll setup the task
			formatExtension.setupTask(task);
		}));

		// create the check and apply control tasks
		TaskProvider<SpotlessApply> applyTask = tasks.register(taskName + APPLY, SpotlessApply.class, task -> {
			task.init(spotlessTask);
			task.setGroup(TASK_GROUP);
			task.setEnabled(ideHook.paths == null);
			task.dependsOn(spotlessTask);
		});
		rootApplyTask.configure(task -> task.dependsOn(ideHook.paths == null ? applyTask : spotlessTask));

		TaskProvider<SpotlessCheck> checkTask = tasks.register(taskName + CHECK, SpotlessCheck.class, task -> {
			task.setGroup(TASK_GROUP);
			task.init(spotlessTask);
			task.setEnabled(ideHook.paths == null);
			task.dependsOn(spotlessTask);

			// if the user runs both, make sure that apply happens first,
			task.mustRunAfter(applyTask);
		});
		rootCheckTask.configure(task -> task.dependsOn(checkTask));

		// create the diagnose task
		TaskProvider<SpotlessDiagnoseTask> diagnoseTask = tasks.register(taskName + DIAGNOSE, SpotlessDiagnoseTask.class, task -> {
			task.source = spotlessTask;
			task.setGroup(TASK_GROUP);
			task.mustRunAfter(BasePlugin.CLEAN_TASK_NAME);
		});
		rootDiagnoseTask.configure(task -> task.dependsOn(diagnoseTask));
	}
}
