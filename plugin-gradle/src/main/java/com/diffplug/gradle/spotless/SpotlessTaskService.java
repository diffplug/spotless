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
import org.gradle.api.tasks.Input;

/** A service which allows looking up a SpotlessTaskImpl from its path. */
class SpotlessTaskService {
	private static final SpotlessTaskService instance = new SpotlessTaskService();

	static SpotlessTaskService instance() {
		return instance;
	}

	private SpotlessTaskService() {}

	private Map<String, SpotlessTaskImpl> map = new HashMap<>();

	synchronized void put(SpotlessTaskImpl task) {
		map.put(task.getPath(), task);
	}

	synchronized SpotlessTaskImpl get(String taskPath) {
		return Objects.requireNonNull(map.get(taskPath), taskPath);
	}

	public static abstract class DependentTask extends DefaultTask {
		private String sourceTaskPath;

		@Input
		public final String getSourceTaskPath() {
			return sourceTaskPath;
		}

		protected final SpotlessTaskImpl source() {
			return instance().get(sourceTaskPath);
		}

		public void link(SpotlessTaskImpl task) {
			sourceTaskPath = task.getPath();
		}
	}
}
