/*
 * Copyright 2020 DiffPlug
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.gradle.api.DefaultTask;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;

/** A service which allows looking up a SpotlessTaskImpl from its path. */
public abstract class SpotlessTaskService implements BuildService<BuildServiceParameters.None> {
	private Map<String, SpotlessTaskImpl> taskPathToTask = new HashMap<>();

	public static abstract class DependentTask extends DefaultTask {
		private String sourceTaskPath;
		private transient SpotlessTaskService taskService;

		@Input
		public final String getSourceTaskPath() {
			return sourceTaskPath;
		}

		@Internal
		public SpotlessTaskService getTaskService() {
			return taskService;
		}

		protected final SpotlessTaskImpl source() {
			return Objects.requireNonNull(taskService.taskPathToTask.get(sourceTaskPath), sourceTaskPath);
		}

		public void link(SpotlessTaskImpl task) {
			sourceTaskPath = task.getPath();
			taskService = task.getProject().getGradle().getSharedServices()
					.registerIfAbsent("SpotlessTaskService", SpotlessTaskService.class, unused -> {}).get();
			if (taskService.taskPathToTask.put(sourceTaskPath, task) == null) {
				task.taskService = taskService;
			}
		}
	}
}
