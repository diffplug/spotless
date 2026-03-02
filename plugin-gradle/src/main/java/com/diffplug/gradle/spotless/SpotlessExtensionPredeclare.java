/*
 * Copyright 2021-2026 DiffPlug
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

import java.util.SortedMap;
import java.util.TreeMap;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;

import com.diffplug.spotless.LazyForwardingEquality;

public class SpotlessExtensionPredeclare extends SpotlessExtension {
	private final SortedMap<String, FormatExtension> toSetup = new TreeMap<>();
	private final RegisterDependenciesTask registerDependenciesTask;

	public SpotlessExtensionPredeclare(Project project, GradleProvisioner.Policy policy) {
		super(project);
		this.registerDependenciesTask = findRegisterDepsTask().get();
		SpotlessTaskService taskService = getSpotlessTaskService().get();
		taskService.isUsingPredeclared = true;
		taskService.predeclaredProvisioner = policy.dedupingProvisioner(project);
		taskService.predeclaredP2Provisioner = policy.dedupingP2Provisioner(project);
		project.afterEvaluate(unused -> toSetup.forEach((name, formatExtension) -> {
			for (Action<FormatExtension> lazyAction : formatExtension.lazyActions) {
				lazyAction.execute(formatExtension);
			}
			registerDependenciesTask.steps.addAll(formatExtension.steps);
			// needed to fix Deemon memory leaks (#1194), but this line came from https://github.com/diffplug/spotless/pull/1206
			LazyForwardingEquality.unlazy(registerDependenciesTask.steps);
		}));
	}

	@Override
	protected void createFormatTasks(String name, FormatExtension formatExtension) {
		toSetup.put(name, formatExtension);
	}

	@Override
	protected void predeclare(GradleProvisioner.Policy policy) {
		throw new UnsupportedOperationException("predeclare can't be called from within `" + EXTENSION_PREDECLARE + "`");
	}

	private TaskProvider<RegisterDependenciesTask> findRegisterDepsTask() {
		try {
			return findRegisterDepsTask(RegisterDependenciesTask.TASK_NAME);
		} catch (Exception e) {
			// in a composite build there can be multiple Spotless plugins on the classpath, and they will each try to register
			// a task on the root project with the same name. That will generate casting errors, which we can catch and try again
			// with an identity-specific identifier.
			// https://github.com/diffplug/spotless/pull/1001 for details
			return findRegisterDepsTask(RegisterDependenciesTask.TASK_NAME + System.identityHashCode(RegisterDependenciesTask.class));
		}
	}

	private TaskProvider<RegisterDependenciesTask> findRegisterDepsTask(String taskName) {
		return project.getTasks().register(taskName, RegisterDependenciesTask.class, RegisterDependenciesTask::setup);
	}

}
