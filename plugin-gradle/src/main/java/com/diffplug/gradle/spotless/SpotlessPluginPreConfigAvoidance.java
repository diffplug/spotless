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
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.util.GradleVersion;

class SpotlessPluginPreConfigAvoidance {
	static final GradleVersion CONFIG_AVOIDANCE_INTRODUCED = GradleVersion.version("4.9");

	static void enforceCheck(SpotlessExtension extension, Project project) {
		Task check = project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME);
		check.dependsOn(extension.rootCheckTask);
	}
}
