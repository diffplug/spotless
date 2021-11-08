/*
 * Copyright 2021 DiffPlug
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
import org.gradle.api.tasks.TaskProvider;

public class SpotlessSetup {
	static final String NAME = "spotlessSetup";

	private final TaskProvider<RegisterDependenciesTask> task;

	public SpotlessSetup(Project project) {
		task = (TaskProvider<RegisterDependenciesTask>) (Object) project.getTasks().named(RegisterDependenciesTask.TASK_NAME);
	}

	public boolean isEnableConfigCacheDaemonLocal() {
		return task.get().getEnableConfigCacheDaemonLocal();
	}

	public boolean setEnableConfigCacheDaemonLocal(boolean enabled) {
		return task.get().enableConfigCacheDaemonLocal = enabled;
	}
}
