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

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

public class SpotlessExtensionModern extends SpotlessExtensionBase {
	private final TaskProvider<RegisterDependenciesTask> registerDependenciesTask;

	public SpotlessExtensionModern(Project project) {
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

		TaskContainer rootProjectTasks = project.getRootProject().getTasks();
		if (!rootProjectTasks.getNames().contains(RegisterDependenciesTask.TASK_NAME)) {
			this.registerDependenciesTask = rootProjectTasks.register(RegisterDependenciesTask.TASK_NAME, RegisterDependenciesTask.class, RegisterDependenciesTask::setup);
		} else {
			this.registerDependenciesTask = rootProjectTasks.named(RegisterDependenciesTask.TASK_NAME, RegisterDependenciesTask.class);
		}

		project.afterEvaluate(unused -> {
			if (enforceCheck) {
				project.getTasks().named(JavaBasePlugin.CHECK_TASK_NAME)
						.configure(task -> task.dependsOn(rootCheckTask));
			}
		});
	}

	final TaskProvider<?> rootCheckTask, rootApplyTask, rootDiagnoseTask;

	@Override
	RegisterDependenciesTask getRegisterDependenciesTask() {
		return registerDependenciesTask.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends FormatExtension> void format(String name, Class<T> clazz, Action<T> configure) {
		maybeCreate(name, clazz).modernLazyActions.add((Action<FormatExtension>) configure);
	}

	@Override
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		boolean isIdeHook = project.hasProperty(IdeHook.PROPERTY);
		TaskContainer tasks = project.getTasks();
		TaskProvider<?> cleanTask = tasks.named(BasePlugin.CLEAN_TASK_NAME);

		// create the SpotlessTask
		String taskName = EXTENSION + SpotlessPlugin.capitalize(name);
		TaskProvider<SpotlessTaskModern> spotlessTask = tasks.register(taskName, SpotlessTaskModern.class, task -> {
			task.setEnabled(!isIdeHook);
			// clean removes the SpotlessCache, so we have to run after clean
			task.mustRunAfter(cleanTask);
		});

		project.afterEvaluate(unused -> {
			spotlessTask.configure(task -> {
				// now that the task is being configured, we execute our actions
				for (Action<FormatExtension> lazyAction : formatExtension.modernLazyActions) {
					lazyAction.execute(formatExtension);
				}
				// and now we'll setup the task
				formatExtension.setupTask(task);
			});
		});

		// create the check and apply control tasks
		TaskProvider<SpotlessApply> applyTask = tasks.register(taskName + APPLY, SpotlessApply.class, task -> {
			task.setEnabled(!isIdeHook);
			task.dependsOn(spotlessTask);
			task.setSpotlessOutDirectory(spotlessTask.get().getOutputDirectory());
			task.linkSource(spotlessTask.get());
		});
		rootApplyTask.configure(task -> {
			task.dependsOn(applyTask);

			if (isIdeHook) {
				// the rootApplyTask is no longer just a marker task, now it does a bit of work itself
				task.doLast(unused -> IdeHook.performHook(spotlessTask.get()));
			}
		});

		TaskProvider<SpotlessCheck> checkTask = tasks.register(taskName + CHECK, SpotlessCheck.class, task -> {
			task.setEnabled(!isIdeHook);
			task.dependsOn(spotlessTask);
			task.setSpotlessOutDirectory(spotlessTask.get().getOutputDirectory());
			task.source = spotlessTask.get();

			// if the user runs both, make sure that apply happens first,
			task.mustRunAfter(applyTask);
		});
		rootCheckTask.configure(task -> task.dependsOn(checkTask));

		// create the diagnose task
		TaskProvider<SpotlessDiagnoseTask> diagnoseTask = tasks.register(taskName + DIAGNOSE, SpotlessDiagnoseTask.class, task -> {
			task.source = spotlessTask.get();
			task.mustRunAfter(cleanTask);
		});
		rootDiagnoseTask.configure(task -> task.dependsOn(diagnoseTask));
	}
}
